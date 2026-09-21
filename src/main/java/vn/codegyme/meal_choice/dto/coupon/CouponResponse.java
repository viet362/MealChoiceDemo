package vn.codegyme.meal_choice.dto.coupon;

import lombok.Builder;
import lombok.Data;
import vn.codegyme.meal_choice.entity.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CouponResponse {

    private Long id;
    private String couponCode;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer usageLimit;
    private Integer usedCount;
    private Boolean isActive;
}