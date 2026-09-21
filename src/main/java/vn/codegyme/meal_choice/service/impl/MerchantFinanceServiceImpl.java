package vn.codegyme.meal_choice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.codegyme.meal_choice.repository.MerchantPayoutRequestRepository;
import vn.codegyme.meal_choice.repository.MerchantSettlementRepository;
import vn.codegyme.meal_choice.service.MerchantFinanceService;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchantFinanceServiceImpl
        implements MerchantFinanceService {

    private final MerchantPayoutRequestRepository
            payoutRequestRepository;

    private final MerchantSettlementRepository
            settlementRepository;


    // ==========================================
    // TỔNG DOANH THU ĐÃ ĐỐI SOÁT XÁC NHẬN (CONFIRMED)
    // ==========================================

    @Override
    public BigDecimal getTotalRevenue(UUID merchantId) {
        BigDecimal confirmedRevenue = settlementRepository
                .getTotalConfirmedSettlementRevenue(merchantId);

        return confirmedRevenue != null
                ? confirmedRevenue
                : BigDecimal.ZERO;
    }


    // ==========================================
    // TỔNG TIỀN ĐÃ ĐƯỢC ADMIN CHUYỂN
    // ==========================================

    @Override
    public BigDecimal getTotalPaidAmount(UUID merchantId) {

        BigDecimal amount =
                payoutRequestRepository
                        .getTotalPaidAmount(merchantId);

        return amount != null
                ? amount
                : BigDecimal.ZERO;
    }


    // ==========================================
    // TỔNG TIỀN ĐANG CHỜ DUYỆT RÚT (PENDING)
    // ==========================================

    @Override
    public BigDecimal getTotalPendingAmount(UUID merchantId) {

        BigDecimal amount =
                payoutRequestRepository
                        .getTotalPendingAmount(merchantId);

        return amount != null
                ? amount
                : BigDecimal.ZERO;
    }


    // ==========================================
    // SỐ DƯ CÓ THỂ RÚT (AVAILABLE BALANCE)
    // Số dư = Doanh thu đã chốt - Đã chuyển - Đang chờ duyệt
    // ==========================================

    @Override
    public BigDecimal getAvailableBalance(UUID merchantId) {

        BigDecimal confirmedRevenue =
                getTotalRevenue(merchantId);

        BigDecimal totalPaid =
                getTotalPaidAmount(merchantId);

        BigDecimal totalPending =
                getTotalPendingAmount(merchantId);

        BigDecimal balance =
                confirmedRevenue.subtract(totalPaid).subtract(totalPending);


        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }

        return balance;
    }
}
