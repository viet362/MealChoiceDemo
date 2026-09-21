package vn.codegyme.meal_choice.dto.settlement;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminSettlementItemDTO {

    private Long settlementId;
    private UUID merchantId;
    private String merchantRestaurantName;
    private String merchantEmail;
    private String merchantPhone;

    private String periodKey;
    private String periodLabel;
    private String periodType;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private BigDecimal totalGrossRevenue;
    private BigDecimal totalDiscount;
    private BigDecimal commissionRate;
    private BigDecimal totalCommissionFee;
    private BigDecimal netRevenue;
    private BigDecimal adjustmentAmount;
    private Long totalOrders;

    private String status;
    private String statusDisplayName;
    private String statusBadgeClass;
    private LocalDateTime confirmedAt;
    private boolean isInProgress;

    private boolean hasClaim;
    private Long claimId;
    private String claimReason;
    private String claimReasonDisplayName;
    private String claimDescription;
    private String claimEvidenceImageUrl;
    private String claimStatus;
    private String claimStatusDisplayName;
    private String claimStatusBadgeClass;
    private LocalDateTime claimCreatedAt;
    private String claimAdminNote;
}
