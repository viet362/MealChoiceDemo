package vn.codegyme.meal_choice.dto.settlement;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminSettlementStatsDTO {
    private long totalSettlements;
    private long totalPendingConfirm;
    private long totalConfirmed;
    private long totalDisputed;
    private long totalActiveClaims;
    private BigDecimal totalPendingPayoutAmount;
}
