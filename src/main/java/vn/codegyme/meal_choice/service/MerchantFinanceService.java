package vn.codegyme.meal_choice.service;


import java.math.BigDecimal;
import java.util.UUID;

public interface MerchantFinanceService {

    BigDecimal getTotalRevenue(UUID merchantId);

    BigDecimal getTotalPaidAmount(UUID merchantId);

    BigDecimal getTotalPendingAmount(UUID merchantId);

    BigDecimal getAvailableBalance(UUID merchantId);
}
