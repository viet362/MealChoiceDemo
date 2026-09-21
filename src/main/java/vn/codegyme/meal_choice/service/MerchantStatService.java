package vn.codegyme.meal_choice.service;

import vn.codegyme.meal_choice.dto.stat.CouponStatDTO;
import vn.codegyme.meal_choice.dto.stat.CustomerStatDTO;
import vn.codegyme.meal_choice.dto.stat.FoodStatDTO;
import vn.codegyme.meal_choice.dto.stat.RevenueStatDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface MerchantStatService {

    List<RevenueStatDTO> getRevenueStats(
            UUID merchantId,
            String type
    );

    List<FoodStatDTO> getFoodStats(
            UUID merchantId
    );

    List<CustomerStatDTO> getCustomerStats(
            UUID merchantId
    );

    CouponStatDTO getCouponStats(
            UUID merchantId
    );

    // Dùng cho Rút tiền / Thanh lý
    BigDecimal getTotalRevenue(
            UUID merchantId
    );
}