package vn.codegyme.meal_choice.dto.stat;

import java.math.BigDecimal;

public record RevenueStatDTO(
        String period,
        Long totalOrders,
        BigDecimal totalRevenue
) {}