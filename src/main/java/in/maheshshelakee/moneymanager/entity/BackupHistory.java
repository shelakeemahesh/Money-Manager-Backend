package in.maheshshelakee.moneymanager.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_backup_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime timestamp;

    private Long size; // size in bytes
    private String type; // FULL or INCREMENTAL
    private String status; // SUCCESS or FAILED
    private String storageDestination; // LOCAL or S3
    private String fileName;
    
    @Column(length = 1024)
    private String filePathOrUrl;
}
