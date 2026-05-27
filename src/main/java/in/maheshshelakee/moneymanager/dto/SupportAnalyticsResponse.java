package in.maheshshelakee.moneymanager.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class SupportAnalyticsResponse {
    private Double averageResolutionTimeHours;
    private Map<String, Long> ticketsByCategory;
    private Map<String, Long> openVsResolvedTrend; // Key: Date string, Value: ticket count
}
