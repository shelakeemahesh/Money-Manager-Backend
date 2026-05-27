package in.maheshshelakee.moneymanager.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class UserFeedbackDto {
    private Long id;
    private String userEmail;
    private String userName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
