package in.maheshshelakee.moneymanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedTransactionsResponse {
    private List<TransactionDto> transactions;
    private long totalCount;
    private int page;
    private int pageSize;
}
