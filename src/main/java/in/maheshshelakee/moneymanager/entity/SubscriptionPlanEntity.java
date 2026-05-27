package in.maheshshelakee.moneymanager.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_subscription_plans")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // e.g., "Free", "Pro", "Enterprise"

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private String billingCycle; // e.g., "MONTHLY", "YEARLY"

    @Column(columnDefinition = "TEXT")
    private String features; // JSON or comma-separated

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;
}
