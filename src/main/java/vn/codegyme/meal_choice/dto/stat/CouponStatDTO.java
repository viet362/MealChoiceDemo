package vn.codegyme.meal_choice.dto.stat;

import java.math.BigDecimal;

public record CouponStatDTO(
        Long totalOrdersWithDiscount,
        BigDecimal totalDiscountAmount,
        BigDecimal totalRevenue
) {
    public BigDecimal discountAmount() {
        return totalDiscountAmount;
    }
}