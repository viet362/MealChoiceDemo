package vn.codegyme.meal_choice.service;


import vn.codegyme.meal_choice.entity.MerchantPayoutRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface MerchantPayoutService {

    MerchantPayoutRequest createWithdrawalRequest(
            UUID merchantId,
            BigDecimal amount
    );

    MerchantPayoutRequest createLiquidationRequest(
            UUID merchantId
    );

    List<MerchantPayoutRequest> getMerchantRequests(
            UUID merchantId
    );

    List<vn.codegyme.meal_choice.dto.payout.MerchantTransactionHistoryDTO> getTransactionHistory(
            UUID merchantId
    );
}
