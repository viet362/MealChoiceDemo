package vn.codegyme.meal_choice.dto.stat;

import java.math.BigDecimal;

public record CustomerStatDTO(
        String customerName,
        String email,
        Long totalOrders,
        BigDecimal totalSpent
) {}