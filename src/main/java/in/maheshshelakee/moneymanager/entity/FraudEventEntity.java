package in.maheshshelakee.moneymanager.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long transactionId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String flagReason; // UNUSUAL_AMOUNT, DUPLICATE, VELOCITY_SPIKE, OFF_HOURS

    @Column(nullable = false)
    private Integer riskScore; // 0 to 100

    @Column(nullable = false)
    private String status; // UNDER_REVIEW, CLEARED, BLOCKED

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime detectedAt;
}
