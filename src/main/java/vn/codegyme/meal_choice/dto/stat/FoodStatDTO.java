package vn.codegyme.meal_choice.dto.stat;

import java.math.BigDecimal;

public record   FoodStatDTO(
        Long foodId,
        String foodName,
        Long totalQuantitySold,
        BigDecimal totalRevenue
) {}