package vn.codegyme.meal_choice.dto.settlement;

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
public class SettlementOrderDTO {
    private Long orderId;
    private String orderCode;
    private LocalDateTime createdAt;
    private String contactName;
    private String contactPhone;
    private BigDecimal subtotalPrice;
    private BigDecimal discountAmount;
    private BigDecimal commissionFee;
    private BigDecimal netAmount;
    private String statusDisplayName;
    private String statusBadgeClass;
}
