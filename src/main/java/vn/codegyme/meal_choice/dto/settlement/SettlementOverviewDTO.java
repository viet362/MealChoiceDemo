package vn.codegyme.meal_choice.dto.settlement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementOverviewDTO {
    private Long settlementId;
    private String periodKey;
    private String periodLabel;
    private String periodType;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // 4 chỉ số tài chính chính
    private BigDecimal totalGrossRevenue;
    private BigDecimal totalDiscount;
    private BigDecimal commissionRate;
    private String commissionRateDisplay;
    private BigDecimal totalCommissionFee;
    private BigDecimal netRevenue;
    private BigDecimal adjustmentAmount;
    private BigDecimal originalNetRevenue;

    private Long totalOrders;
    private String status;
    private String statusDisplayName;
    private String statusBadgeClass;
    private LocalDateTime confirmedAt;
    private boolean actionable;
    private boolean isInProgress;

    // Chống đối soát trùng lặp giữa Tuần và Tháng
    private boolean hasOverlap;
    private String overlapMessage;

    // Thông tin khiếu nại nếu có
    private String claimStatus;
    private String claimReason;
    private String claimDescription;
    private String claimEvidenceUrl;
    private String claimAdminNote;
    private LocalDateTime claimCreatedAt;

    @Builder.Default
    private List<SettlementOrderDTO> orders = new ArrayList<>();
}
