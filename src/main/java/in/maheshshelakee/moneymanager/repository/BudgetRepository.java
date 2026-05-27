package in.maheshshelakee.moneymanager.repository;

import in.maheshshelakee.moneymanager.entity.BudgetEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BudgetRepository extends JpaRepository<BudgetEntity, Long> {

    List<BudgetEntity> findByUserId(Long userId);
    
    Page<BudgetEntity> findAll(Pageable pageable);
}
