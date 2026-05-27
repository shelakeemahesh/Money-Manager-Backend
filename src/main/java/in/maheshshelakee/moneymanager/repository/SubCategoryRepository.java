package in.maheshshelakee.moneymanager.repository;

import in.maheshshelakee.moneymanager.entity.CategoryEntity;
import in.maheshshelakee.moneymanager.entity.User;
import in.maheshshelakee.moneymanager.entity.SubCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubCategoryRepository extends JpaRepository<SubCategoryEntity, Long> {

    List<SubCategoryEntity> findByCategoryOrderByCreatedAtDesc(CategoryEntity category);

    Optional<SubCategoryEntity> findByIdAndCategoryUser(Long id, User user);

    long countByCategory(CategoryEntity category);
}
