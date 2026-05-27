package in.maheshshelakee.moneymanager.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_notification_templates")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String eventName; // e.g., "BUDGET_EXCEEDED", "SUSPICIOUS_TRANSACTION", "SUBSCRIPTION_RENEWAL"

    @Column(nullable = false)
    private String subjectTemplate;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String bodyTemplate;
}
