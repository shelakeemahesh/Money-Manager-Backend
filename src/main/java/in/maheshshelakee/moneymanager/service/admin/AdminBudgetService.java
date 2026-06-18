package in.maheshshelakee.moneymanager.service.admin;

import in.maheshshelakee.moneymanager.dto.BudgetDto;
import in.maheshshelakee.moneymanager.dto.HeatmapDto;
import in.maheshshelakee.moneymanager.entity.BudgetEntity;
import in.maheshshelakee.moneymanager.entity.ExpenseEntity;
import in.maheshshelakee.moneymanager.entity.User;
import in.maheshshelakee.moneymanager.repository.BudgetRepository;
import in.maheshshelakee.moneymanager.repository.ExpenseRepository;
import in.maheshshelakee.moneymanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminBudgetService {

    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    @Value("${openai.api.key:dummy-key}")
    private String openAiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional(readOnly = true)
    public Page<BudgetDto> getAllBudgets(Pageable pageable) {
        Page<BudgetEntity> budgetPage = budgetRepository.findAll(pageable);

        List<BudgetDto> dtos = budgetPage.getContent().stream().map(this::convertToDto).collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, budgetPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<HeatmapDto> getBudgetHeatmap() {
        List<BudgetEntity> allBudgets = budgetRepository.findAll();
        Map<String, HeatmapDto> heatmapMap = new HashMap<>();

        for (BudgetEntity budget : allBudgets) {
            BudgetDto dto = convertToDto(budget);
            if ("OVER_BUDGET".equals(dto.getStatus())) {
                String category = dto.getCategory();
                heatmapMap.putIfAbsent(category, HeatmapDto.builder().category(category).overBudgetCount(0L).totalOverBudgetAmount(0.0).build());

                HeatmapDto hm = heatmapMap.get(category);
                hm.setOverBudgetCount(hm.getOverBudgetCount() + 1);
                hm.setTotalOverBudgetAmount(hm.getTotalOverBudgetAmount() + Math.abs(dto.getRemainingAmount()));
            }
        }

        return new ArrayList<>(heatmapMap.values());
    }

    @Transactional
    public String generateAIRecommendation(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<BudgetEntity> budgets = budgetRepository.findByUserId(userId);
        List<BudgetDto> budgetDtos = budgets.stream().map(this::convertToDto).toList();

        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an expert AI financial advisor. Analyze the following budget data for a user and provide actionable, brief recommendations (max 3 short paragraphs).\n\n");
        prompt.append("User: ").append(user.getFullName()).append("\n");
        
        if (budgetDtos.isEmpty()) {
            prompt.append("The user has no budgets defined. Advise them to set up essential budgets.\n");
        } else {
            prompt.append("Budgets & Spending:\n");
            for (BudgetDto b : budgetDtos) {
                prompt.append("- ").append(b.getCategory()).append(": ")
                        .append("Budget: ₹").append(b.getBudgetedAmount())
                        .append(", Spent: ₹").append(b.getSpentAmount())
                        .append(" (Status: ").append(b.getStatus()).append(")\n");
            }
        }

        // Call OpenAI API
        try {
            if ("dummy-key".equals(openAiApiKey) || openAiApiKey.isBlank()) {
                log.warn("OpenAI API key not configured, returning simulated response.");
                return simulateOpenAiResponse(budgetDtos);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAiApiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4o");
            
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt.toString());
            requestBody.put("messages", Collections.singletonList(message));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity("https://api.openai.com/v1/chat/completions", entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                    return (String) msg.get("content");
                }
            }
            return "Unable to parse AI recommendation at this time.";

        } catch (Exception e) {
            log.error("Failed to generate AI recommendation: {}", e.getMessage());
            return "AI Recommendation engine is currently unavailable due to network or configuration errors.";
        }
    }

    private String simulateOpenAiResponse(List<BudgetDto> budgetDtos) {
        long overBudgetCount = budgetDtos.stream().filter(b -> "OVER_BUDGET".equals(b.getStatus())).count();
        if (overBudgetCount > 0) {
            return "Based on your spending patterns, you are currently over budget in " + overBudgetCount + 
                   " categories. We highly recommend auditing your recent transactions in these areas to identify unnecessary expenditures. Consider temporarily freezing non-essential spending until the next cycle.";
        } else if (!budgetDtos.isEmpty()) {
            return "Excellent financial discipline! Your spending aligns well with your targets. Consider allocating your current surplus towards an emergency fund or a high-yield savings account.";
        } else {
            return "You haven't established any budget targets yet. Try utilizing the AI Budget Planner to generate an initial 50/30/20 baseline allocation.";
        }
    }

    private BudgetDto convertToDto(BudgetEntity budget) {
        LocalDate now = LocalDate.now();
        LocalDate startOfPeriod = "WEEKLY".equals(budget.getPeriod()) 
                ? now.minusDays(now.getDayOfWeek().getValue() - 1) 
                : now.withDayOfMonth(1);

        List<ExpenseEntity> expenses = expenseRepository.findAll().stream()
                .filter(e -> e.getUser().getId().equals(budget.getUser().getId())
                        && e.getCategory().equalsIgnoreCase(budget.getCategory())
                        && !e.getExpenseDate().isBefore(startOfPeriod))
                .collect(Collectors.toList());

        double spentAmount = expenses.stream().mapToDouble(ExpenseEntity::getAmount).sum();
        double remainingAmount = budget.getAmount() - spentAmount;
        
        String status = "ON_TRACK";
        double percentage = (spentAmount / budget.getAmount()) * 100;
        
        if (percentage > 100) {
            status = "OVER_BUDGET";
        } else if (percentage >= 80) {
            status = "CRITICAL";
        }

        return BudgetDto.builder()
                .id(budget.getId())
                .userId(budget.getUser().getId())
                .userName(budget.getUser().getFullName())
                .category(budget.getCategory())
                .budgetedAmount(budget.getAmount())
                .spentAmount(spentAmount)
                .remainingAmount(remainingAmount)
                .period(budget.getPeriod())
                .status(status)
                .build();
    }
}
