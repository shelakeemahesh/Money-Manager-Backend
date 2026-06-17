package in.maheshshelakee.moneymanager.repository;

import in.maheshshelakee.moneymanager.entity.FriendExpense;
import in.maheshshelakee.moneymanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FriendExpenseRepository extends JpaRepository<FriendExpense, Long> {
    List<FriendExpense> findByUserOrderByExpenseDateDesc(User user);
    
    @Query("SELECT fe.friendName, SUM(fe.amount) FROM FriendExpense fe WHERE fe.user = :user GROUP BY fe.friendName ORDER BY SUM(fe.amount) DESC")
    List<Object[]> getFriendSpendBreakdown(@Param("user") User user);
    
    @Query("SELECT fe.category, SUM(fe.amount) FROM FriendExpense fe WHERE fe.user = :user GROUP BY fe.category")
    List<Object[]> getCategorySpendBreakdown(@Param("user") User user);

    List<FriendExpense> findByExpenseDateBetweenOrderByExpenseDateDesc(java.time.LocalDate start, java.time.LocalDate end);
}

