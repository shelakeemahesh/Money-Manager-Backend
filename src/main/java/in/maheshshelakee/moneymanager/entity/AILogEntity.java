package in.maheshshelakee.moneymanager.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_ai_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AILogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String endpoint;

    @Column(columnDefinition = "TEXT")
    private String requestPayload;

    @Column(nullable = false)
    private Integer responseStatus;

    @Column(nullable = false)
    private Long executionTimeMs;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime timestamp;
}
