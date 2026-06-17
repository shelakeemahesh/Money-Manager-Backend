package in.maheshshelakee.moneymanager.repository;

import in.maheshshelakee.moneymanager.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FriendTransactionRepository extends JpaRepository<FriendTransaction, Long> {

    List<FriendTransaction> findByUserAndStatusOrderByTransactionDateDesc(User user, FriendTransactionStatus status);

    @Query("SELECT ft FROM FriendTransaction ft WHERE ft.user = :user " +
            "AND (:type IS NULL OR ft.type = :type) " +
            "AND (:status IS NULL OR ft.status = :status) " +
            "AND (:friendName IS NULL OR :friendName = '' OR LOWER(ft.friendName) LIKE LOWER(CONCAT('%', :friendName, '%'))) " +
            "ORDER BY ft.transactionDate DESC, ft.id DESC")
    List<FriendTransaction> findFilteredTransactions(
            @Param("user") User user,
            @Param("type") FriendTransactionType type,
            @Param("status") FriendTransactionStatus status,
            @Param("friendName") String friendName);

    @Query("SELECT ft.friendName, ft.type, SUM(ft.amount) " +
            "FROM FriendTransaction ft " +
            "WHERE ft.user = :user " +
            "GROUP BY ft.friendName, ft.type")
    List<Object[]> getFriendBalancesRaw(@Param("user") User user);
}
