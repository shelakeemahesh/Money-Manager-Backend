package in.maheshshelakee.moneymanager.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_ai_settings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AISettingsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Builder.Default
    private Boolean globalAiEnabled = true;

    @Column(nullable = false)
    @Builder.Default
    private Double confidenceThreshold = 0.75;

    @Column(nullable = false)
    @Builder.Default
    private Integer predictionWindowDays = 14;
}
