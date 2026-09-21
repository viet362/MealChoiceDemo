package vn.codegyme.meal_choice.dto.payout;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantTransactionHistoryDTO {
    private String id;
    private LocalDateTime createdAt;
    private String type; // "RECEIVE_SETTLEMENT", "WITHDRAWAL", "LIQUIDATION"
    private String typeDisplayName;
    private String typeBadgeClass;
    private BigDecimal amount;
    private boolean isIncome; // true = nhận (+), false = rút (-)
    private String accountInfo;
    private String periodLabel;
    private String status; // "COMPLETED", "PENDING", "REJECTED"
    private String statusDisplayName;
    private String statusBadgeClass;
    private LocalDateTime completedAt;
    private LocalDateTime rejectedAt;
    private String transferProofUrl;
    private String adminNote;
}
