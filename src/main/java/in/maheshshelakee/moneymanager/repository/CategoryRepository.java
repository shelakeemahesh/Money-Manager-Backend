package in.maheshshelakee.moneymanager.repository;

import in.maheshshelakee.moneymanager.entity.CategoryEntity;
import in.maheshshelakee.moneymanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    @Query("SELECT c FROM CategoryEntity c LEFT JOIN FETCH c.subcategories WHERE c.user = :user ORDER BY c.createdAt DESC")
    List<CategoryEntity> findByUserWithSubcategories(@Param("user") User user);

    @Query("SELECT c FROM CategoryEntity c LEFT JOIN FETCH c.subcategories " +
           "WHERE (c.user = :user OR (c.user IS NULL AND c.globalTemplate = true)) " +
           "AND (c.archived = false OR c.archived IS NULL) " +
           "ORDER BY c.createdAt DESC")
    List<CategoryEntity> findActiveByUserOrGlobal(@Param("user") User user);

    @Query("SELECT c FROM CategoryEntity c LEFT JOIN FETCH c.subcategories " +
           "WHERE c.user IS NULL AND (c.archived = false OR c.archived IS NULL) " +
           "ORDER BY c.createdAt DESC")
    List<CategoryEntity> findActiveGlobals();

    Optional<CategoryEntity> findByIdAndUser(Long id, User user);

    boolean existsByNameAndTypeAndUser(String name, String type, User user);

    long countByUser(User user);
}
