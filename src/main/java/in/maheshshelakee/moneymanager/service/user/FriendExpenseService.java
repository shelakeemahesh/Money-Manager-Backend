package in.maheshshelakee.moneymanager.service.user;

import in.maheshshelakee.moneymanager.dto.FriendExpenseDTO;
import in.maheshshelakee.moneymanager.dto.FriendStatsResponse;
import in.maheshshelakee.moneymanager.entity.FriendExpense;
import in.maheshshelakee.moneymanager.entity.User;
import in.maheshshelakee.moneymanager.repository.FriendExpenseRepository;
import in.maheshshelakee.moneymanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendExpenseService {

    private final FriendExpenseRepository friendExpenseRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<FriendExpenseDTO> getAllFriendExpenses(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return friendExpenseRepository.findByUserOrderByExpenseDateDesc(user).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public FriendExpenseDTO createFriendExpense(FriendExpenseDTO dto, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        FriendExpense expense = FriendExpense.builder()
                .friendName(dto.getFriendName().trim())
                .amount(dto.getAmount())
                .category(dto.getCategory())
                .description(dto.getDescription())
                .expenseDate(dto.getExpenseDate())
                .user(user)
                .build();
        
        expense = friendExpenseRepository.save(expense);
        return toDTO(expense);
    }

    @Transactional
    public FriendExpenseDTO updateFriendExpense(Long id, FriendExpenseDTO dto, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        FriendExpense expense = friendExpenseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found"));

        if (!isOwnerOrShared(expense.getUser(), user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        expense.setFriendName(dto.getFriendName().trim());
        expense.setAmount(dto.getAmount());
        expense.setCategory(dto.getCategory());
        expense.setDescription(dto.getDescription());
        expense.setExpenseDate(dto.getExpenseDate());

        expense = friendExpenseRepository.save(expense);
        return toDTO(expense);
    }

    @Transactional
    public void deleteFriendExpense(Long id, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        FriendExpense expense = friendExpenseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found"));

        if (!isOwnerOrShared(expense.getUser(), user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        friendExpenseRepository.delete(expense);
    }

    @Transactional(readOnly = true)
    public FriendStatsResponse getFriendStats(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<FriendExpense> all = friendExpenseRepository.findByUserOrderByExpenseDateDesc(user);
        Double total = all.stream().mapToDouble(FriendExpense::getAmount).sum();

        List<Object[]> friendBreakdownRaw = friendExpenseRepository.getFriendSpendBreakdown(user);
        List<FriendStatsResponse.FriendSpendAggregate> friendBreakdown = friendBreakdownRaw.stream()
                .map(row -> new FriendStatsResponse.FriendSpendAggregate((String) row[0], (Double) row[1]))
                .collect(Collectors.toList());

        List<Object[]> catBreakdownRaw = friendExpenseRepository.getCategorySpendBreakdown(user);
        Map<String, Double> categoryBreakdown = catBreakdownRaw.stream()
                .collect(Collectors.toMap(row -> (String) row[0], row -> (Double) row[1]));

        return new FriendStatsResponse(total, friendBreakdown, categoryBreakdown);
    }

    private FriendExpenseDTO toDTO(FriendExpense expense) {
        return FriendExpenseDTO.builder()
                .id(expense.getId())
                .friendName(expense.getFriendName())
                .amount(expense.getAmount())
                .category(expense.getCategory())
                .description(expense.getDescription())
                .expenseDate(expense.getExpenseDate())
                .build();
    }

    private boolean isOwnerOrShared(User owner, User currentUser) {
        if (owner.getId().equals(currentUser.getId())) {
            return true;
        }
        String ownerEmail = owner.getEmail();
        String currentEmail = currentUser.getEmail();
        return (ownerEmail.equals("shelakemahesh024@gmail.com") || ownerEmail.equals("shelakemahesh91@gmail.com"))
                && (currentEmail.equals("shelakemahesh024@gmail.com") || currentEmail.equals("shelakemahesh91@gmail.com"));
    }
}
