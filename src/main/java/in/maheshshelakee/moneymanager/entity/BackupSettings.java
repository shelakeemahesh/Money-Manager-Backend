package in.maheshshelakee.moneymanager.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_backup_settings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String frequency; // DAILY or WEEKLY

    @Column(nullable = false)
    private Integer retentionPeriodDays; // 7, 14, 30, 90

    @Column(nullable = false)
    private String storageDestination; // LOCAL or S3

    private String awsBucket;
    private String awsAccessKey;
    private String awsSecretKey;
    private String awsRegion;
}
