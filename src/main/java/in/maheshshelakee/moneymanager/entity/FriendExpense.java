package in.maheshshelakee.moneymanager.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_friend_expenses", indexes = {
        @Index(name = "idx_friend_exp_user_date", columnList = "user_id, expenseDate"),
        @Index(name = "idx_friend_exp_user_name", columnList = "user_id, friendName")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String friendName;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String category;

    private String description;

    @Column(nullable = false)
    private LocalDate expenseDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
