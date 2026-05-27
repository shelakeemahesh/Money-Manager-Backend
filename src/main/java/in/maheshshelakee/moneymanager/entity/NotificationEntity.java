package in.maheshshelakee.moneymanager.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String title;
    private String message;
    private Boolean isRead;

    private String status; // PENDING, DELIVERED, FAILED
    private LocalDateTime scheduledAt;
    private String recipientType;
    private Integer recipientCount;
    private String recipientValue;
    private String subject;
    @Column(columnDefinition = "TEXT")
    private String messageBody;
    private String type; // EMAIL, PUSH, IN_APP
    @Builder.Default
    private Integer retryCount = 0;
    private LocalDateTime sentAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
