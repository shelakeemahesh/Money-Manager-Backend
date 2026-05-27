package in.maheshshelakee.moneymanager.repository;

import in.maheshshelakee.moneymanager.entity.User;
import in.maheshshelakee.moneymanager.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findByEmailOrPhoneNumber(String email, String phoneNumber);

    long countByStatus(UserStatus status);

    @Query("SELECT u FROM User u WHERE " +
           "(:search IS NULL OR :search = '' " +
           "OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.phoneNumber) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:role IS NULL OR u.role = :role) " +
           "AND (:status IS NULL OR u.status = :status)")
    Page<User> findAllFiltered(
            @Param("search") String search,
            @Param("role") in.maheshshelakee.moneymanager.entity.Role role,
            @Param("status") UserStatus status,
            Pageable pageable);
}
