package in.maheshshelakee.moneymanager.service;

import in.maheshshelakee.moneymanager.entity.ExpenseEntity;
import in.maheshshelakee.moneymanager.entity.IncomeEntity;
import in.maheshshelakee.moneymanager.entity.User;
import in.maheshshelakee.moneymanager.entity.UserStatus;
import in.maheshshelakee.moneymanager.repository.ExpenseRepository;
import in.maheshshelakee.moneymanager.repository.IncomeRepository;
import in.maheshshelakee.moneymanager.repository.UserRepository;
import in.maheshshelakee.moneymanager.repository.UserSubscriptionRepository;
import in.maheshshelakee.moneymanager.entity.UserSubscriptionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import in.maheshshelakee.moneymanager.entity.FriendExpense;
import in.maheshshelakee.moneymanager.repository.FriendExpenseRepository;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminAnalyticsService {

    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final FriendExpenseRepository friendExpenseRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getAnalytics(LocalDate fromDate, LocalDate toDate) {
        LocalDate start = (fromDate != null) ? fromDate : LocalDate.now().minusDays(30);
        LocalDate end = (toDate != null) ? toDate : LocalDate.now();

        List<User> allUsers = userRepository.findAll();
        List<ExpenseEntity> allExpenses = expenseRepository.findAll();
        List<IncomeEntity> allIncomes = incomeRepository.findAll();
        List<FriendExpense> allFriendExpenses = friendExpenseRepository.findAll();
        List<UserSubscriptionEntity> allSubscriptions = userSubscriptionRepository.findAll();

        long totalUsers = allUsers.size();

        // Total Transactions Count & Volume
        long totalTransactionsCount = allExpenses.size() + allIncomes.size() + allFriendExpenses.size();
        double totalVolume = allExpenses.stream().mapToDouble(ExpenseEntity::getAmount).sum()
                + allIncomes.stream().mapToDouble(IncomeEntity::getAmount).sum()
                + allFriendExpenses.stream().mapToDouble(FriendExpense::getAmount).sum();

        // Platform Revenue Dynamic Calculation based on active user subscriptions in DB:
        double totalPlatformRevenue = allSubscriptions.stream()
                .filter(sub -> "ACTIVE".equalsIgnoreCase(sub.getStatus()))
                .mapToDouble(sub -> sub.getPlan().getPrice())
                .sum();

        // Monthly Active Users (MAU) - count unique users who logged any transaction in the last 30 days
        LocalDate mauThreshold = LocalDate.now().minusDays(30);
        Set<Long> activeUserIds = new HashSet<>();
        allExpenses.stream()
                .filter(e -> !e.getExpenseDate().isBefore(mauThreshold))
                .forEach(e -> activeUserIds.add(e.getUser().getId()));
        allIncomes.stream()
                .filter(i -> i.getDate() != null && !i.getDate().isBefore(mauThreshold))
                .forEach(i -> activeUserIds.add(i.getUser().getId()));
        allFriendExpenses.stream()
                .filter(fe -> !fe.getExpenseDate().isBefore(mauThreshold))
                .forEach(fe -> activeUserIds.add(fe.getUser().getId()));
        long mau = activeUserIds.size();
        if (mau == 0) {
            mau = allUsers.stream().filter(u -> u.getStatus() == UserStatus.ACTIVE).count();
        }

        // 1. Revenue Trends: Weekly and Monthly
        List<Map<String, Object>> weeklyRevenue = new ArrayList<>();
        List<Map<String, Object>> monthlyRevenue = new ArrayList<>();
        
        LocalDate now = LocalDate.now();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM yyyy");

        // Weekly (6 weeks)
        for (int i = 5; i >= 0; i--) {
            LocalDate weekStart = now.minusWeeks(i).with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            LocalDate weekEnd = weekStart.plusDays(6);
            String label = "Wk " + weekStart.format(DateTimeFormatter.ofPattern("w"));
            
            double rev = allSubscriptions.stream()
                    .filter(sub -> !sub.getStartDate().isAfter(weekEnd) && (sub.getRenewalDate() == null || !sub.getRenewalDate().isBefore(weekStart)))
                    .mapToDouble(sub -> sub.getPlan().getPrice())
                    .sum();
            
            Map<String, Object> point = new HashMap<>();
            point.put("date", label);
            point.put("revenue", Math.round(rev));
            weeklyRevenue.add(point);
        }

        // Monthly (6 months)
        for (int i = 5; i >= 0; i--) {
            LocalDate monthStart = now.minusMonths(i).with(java.time.temporal.TemporalAdjusters.firstDayOfMonth());
            LocalDate monthEnd = monthStart.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());
            String label = monthStart.format(monthFormatter);
            
            double rev = allSubscriptions.stream()
                    .filter(sub -> !sub.getStartDate().isAfter(monthEnd) && (sub.getRenewalDate() == null || !sub.getRenewalDate().isBefore(monthStart)))
                    .mapToDouble(sub -> sub.getPlan().getPrice())
                    .sum();
            
            Map<String, Object> point = new HashMap<>();
            point.put("date", label);
            point.put("revenue", Math.round(rev));
            monthlyRevenue.add(point);
        }

        Map<String, Object> revenueTrends = new HashMap<>();
        revenueTrends.put("weekly", weeklyRevenue);
        revenueTrends.put("monthly", monthlyRevenue);

        // 2. Expense Category Distribution (within filter date range)
        Map<String, Double> categorySums = allExpenses.stream()
                .filter(e -> !e.getExpenseDate().isBefore(start) && !e.getExpenseDate().isAfter(end))
                .collect(Collectors.groupingBy(
                        ExpenseEntity::getCategory,
                        Collectors.summingDouble(ExpenseEntity::getAmount)
                ));

        allFriendExpenses.stream()
                .filter(fe -> !fe.getExpenseDate().isBefore(start) && !fe.getExpenseDate().isAfter(end))
                .forEach(fe -> {
                    categorySums.merge(fe.getCategory(), fe.getAmount(), Double::sum);
                });

        List<Map<String, Object>> expenseCategories = categorySums.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("category", entry.getKey());
                    map.put("amount", entry.getValue());
                    return map;
                })
                .sorted((a, b) -> Double.compare((Double) b.get("amount"), (Double) a.get("amount")))
                .collect(Collectors.toList());

        // 3. Income vs Expense Ratio (within filter date range)
        double rangeIncome = allIncomes.stream()
                .filter(i -> i.getDate() != null && !i.getDate().isBefore(start) && !i.getDate().isAfter(end))
                .mapToDouble(IncomeEntity::getAmount)
                .sum();
        double rangeExpense = allExpenses.stream()
                .filter(e -> !e.getExpenseDate().isBefore(start) && !e.getExpenseDate().isAfter(end))
                .mapToDouble(ExpenseEntity::getAmount)
                .sum()
                + allFriendExpenses.stream()
                .filter(fe -> !fe.getExpenseDate().isBefore(start) && !fe.getExpenseDate().isAfter(end))
                .mapToDouble(FriendExpense::getAmount)
                .sum();

        List<Map<String, Object>> incomeVsExpense = List.of(
                Map.of("name", "Income", "value", rangeIncome),
                Map.of("name", "Expense", "value", rangeExpense)
        );

        // 4. Platform Growth Chart: User registrations accumulated over time
        List<Map<String, Object>> platformGrowth = new ArrayList<>();
        // Group registrations by day/month and construct cumulative growth
        Map<LocalDate, Long> signupsByDate = allUsers.stream()
                .map(u -> u.getCreatedAt() != null ? u.getCreatedAt().toLocalDate() : LocalDate.now().minusDays(10))
                .collect(Collectors.groupingBy(d -> d, Collectors.counting()));

        List<LocalDate> sortedDates = signupsByDate.keySet().stream().sorted().collect(Collectors.toList());
        long cumulative = 0;
        DateTimeFormatter growthFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (LocalDate date : sortedDates) {
            cumulative += signupsByDate.get(date);
            platformGrowth.add(Map.of(
                    "date", date.format(growthFormatter),
                    "count", cumulative
            ));
        }

        // Fallback if no users signup dates
        if (platformGrowth.isEmpty()) {
            platformGrowth.add(Map.of("date", LocalDate.now().minusDays(1).format(growthFormatter), "count", 1L));
            platformGrowth.add(Map.of("date", LocalDate.now().format(growthFormatter), "count", totalUsers));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("totalRegisteredUsers", totalUsers);
        data.put("totalTransactionsCount", totalTransactionsCount);
        data.put("totalTransactionsVolume", totalVolume);
        data.put("totalPlatformRevenue", totalPlatformRevenue);
        data.put("monthlyActiveUsers", mau);
        data.put("revenueTrends", revenueTrends);
        data.put("expenseCategories", expenseCategories);
        data.put("incomeVsExpense", incomeVsExpense);
        data.put("platformGrowth", platformGrowth);

        return data;
    }
}
