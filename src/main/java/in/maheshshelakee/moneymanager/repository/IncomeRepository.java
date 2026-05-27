package in.maheshshelakee.moneymanager.repository;

import in.maheshshelakee.moneymanager.entity.IncomeEntity;
import in.maheshshelakee.moneymanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IncomeRepository extends JpaRepository<IncomeEntity, Long> {

    List<IncomeEntity> findByUserOrderByDateDesc(User user);

    Optional<IncomeEntity> findByIdAndUser(Long id, User user);

    List<IncomeEntity> findByUserAndDateBetweenOrderByDateDesc(User user, LocalDate start, LocalDate end);

    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM IncomeEntity i")
    Double sumAllIncomes();

    // Added: per-user income sum for future dashboard use
    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM IncomeEntity i WHERE i.user = :user")
    Double sumAmountByUser(@Param("user") User user);

    List<IncomeEntity> findByDateBetweenOrderByDateDesc(LocalDate startDate, LocalDate endDate);
}
