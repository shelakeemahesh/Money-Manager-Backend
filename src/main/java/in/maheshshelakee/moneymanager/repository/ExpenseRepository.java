package in.maheshshelakee.moneymanager.repository;

import in.maheshshelakee.moneymanager.entity.ExpenseEntity;
import in.maheshshelakee.moneymanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<ExpenseEntity, Long> {

    List<ExpenseEntity> findByUserOrderByExpenseDateDesc(User user);

    Optional<ExpenseEntity> findByIdAndUser(Long id, User user);

    List<ExpenseEntity> findByUserAndExpenseDateBetweenOrderByExpenseDateDesc(
            User user, LocalDate startDate, LocalDate endDate);

    List<ExpenseEntity> findByUserAndCategoryIgnoreCaseOrderByExpenseDateDesc(
            User user, String category);

    List<ExpenseEntity> findByUserAndCategoryIgnoreCaseAndExpenseDateBetweenOrderByExpenseDateDesc(
            User user, String category, LocalDate startDate, LocalDate endDate);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM ExpenseEntity e WHERE e.user = :user")
    Double sumAmountByUser(@Param("user") User user);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM ExpenseEntity e")
    Double sumAllExpenses();

    List<ExpenseEntity> findByExpenseDateBetweenOrderByExpenseDateDesc(LocalDate startDate, LocalDate endDate);
}
