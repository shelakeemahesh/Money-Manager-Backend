package in.maheshshelakee.moneymanager.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_reports")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String reportType; // FINANCIAL_SUMMARY, USER_ACTIVITY, TRANSACTION_AUDIT, BUDGET_COMPLIANCE

    @Column(nullable = false)
    private String dateRangeFrom;

    @Column(nullable = false)
    private String dateRangeTo;

    @Column(nullable = false)
    private String format; // CSV, PDF

    private String filePath;

    @Column(nullable = false)
    private String fileName;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime generatedAt;

    private String generatedBy;

    @Column(nullable = false)
    private String status; // PENDING, COMPLETED, FAILED
}
