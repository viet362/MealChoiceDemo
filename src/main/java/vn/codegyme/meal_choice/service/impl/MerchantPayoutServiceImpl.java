package vn.codegyme.meal_choice.service.impl;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegyme.meal_choice.dto.payout.MerchantTransactionHistoryDTO;
import vn.codegyme.meal_choice.entity.*;
import vn.codegyme.meal_choice.repository.MerchantPayoutRequestRepository;
import vn.codegyme.meal_choice.repository.MerchantRepository;
import vn.codegyme.meal_choice.repository.MerchantSettlementRepository;
import vn.codegyme.meal_choice.service.MerchantFinanceService;
import vn.codegyme.meal_choice.service.MerchantPayoutService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class MerchantPayoutServiceImpl
        implements MerchantPayoutService {

    private static final BigDecimal MIN_TOTAL_REVENUE =
            new BigDecimal("1000");


    private final MerchantRepository merchantRepository;

    private final MerchantPayoutRequestRepository
            payoutRequestRepository;

    private final MerchantSettlementRepository
            settlementRepository;

    private final MerchantFinanceService
            merchantFinanceService;


    // =====================================================
    // RÚT TIỀN
    // =====================================================

    @Override
    public MerchantPayoutRequest createWithdrawalRequest(
            UUID merchantId,
            BigDecimal amount
    ) {

        Merchant merchant =
                getMerchant(merchantId);


        validateMerchantCanRequest(
                merchant
        );


        validateBankAccount(
                merchant
        );


        // ======================================
        // TỔNG DOANH THU PHẢI > 1000
        // ======================================

        BigDecimal totalRevenue =
                merchantFinanceService
                        .getTotalRevenue(
                                merchantId
                        );


        if (totalRevenue.compareTo(
                MIN_TOTAL_REVENUE
        ) <= 0) {

            throw new IllegalArgumentException(
                    "Merchant chỉ được rút tiền khi có doanh thu từ các kỳ đối soát đã xác nhận (lớn hơn 1.000 VNĐ)."
            );
        }


        validateNoPendingRequest(
                merchantId
        );


        // ======================================
        // SỐ DƯ KHẢ DỤNG
        // ======================================

        BigDecimal availableBalance =
                merchantFinanceService
                        .getAvailableBalance(
                                merchantId
                        );


        if (amount == null
                || amount.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            throw new IllegalArgumentException(
                    "Số tiền rút phải lớn hơn 0."
            );
        }


        if (amount.compareTo(
                availableBalance
        ) > 0) {

            throw new IllegalArgumentException(
                    "Số tiền rút vượt quá số dư khả dụng."
            );
        }


        // ======================================
        // TẠO YÊU CẦU
        // ======================================

        MerchantPayoutRequest request =
                MerchantPayoutRequest
                        .builder()

                        .merchant(
                                merchant
                        )

                        .type(
                                PayoutRequestType.WITHDRAWAL
                        )

                        .amount(
                                amount
                        )

                        .bankName(
                                merchant.getBankName()
                        )

                        .bankAccountNumber(
                                merchant.getBankAccountNumber()
                        )

                        .status(
                                PayoutRequestStatus.PENDING
                        )

                        .build();


        return payoutRequestRepository
                .save(request);
    }


    // =====================================================
    // THANH LÝ HỢP ĐỒNG
    // =====================================================

    @Override
    public MerchantPayoutRequest createLiquidationRequest(
            UUID merchantId
    ) {

        Merchant merchant =
                getMerchant(
                        merchantId
                );


        validateMerchantCanRequest(
                merchant
        );


        validateBankAccount(
                merchant
        );


        validateNoPendingRequest(
                merchantId
        );


        // ======================================
        // KHÔNG KIỂM TRA MỐC 100 TRIỆU
        //
        // THANH LÝ = LẤY TOÀN BỘ SỐ DƯ
        // ======================================

        BigDecimal availableBalance =
                merchantFinanceService
                        .getAvailableBalance(
                                merchantId
                        );


        if (availableBalance.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            throw new IllegalArgumentException(
                    "Merchant không còn số dư để thanh lý."
            );
        }


        MerchantPayoutRequest request =
                MerchantPayoutRequest
                        .builder()

                        .merchant(
                                merchant
                        )

                        .type(
                                PayoutRequestType.LIQUIDATION
                        )

                        .amount(
                                availableBalance
                        )

                        .bankName(
                                merchant.getBankName()
                        )

                        .bankAccountNumber(
                                merchant.getBankAccountNumber()
                        )

                        .status(
                                PayoutRequestStatus.PENDING
                        )

                        .build();


        return payoutRequestRepository
                .save(request);
    }


    // =====================================================
    // LỊCH SỬ
    // =====================================================

    @Override
    @Transactional(readOnly = true)
    public List<MerchantPayoutRequest>
    getMerchantRequests(
            UUID merchantId
    ) {

        return payoutRequestRepository
                .findByMerchant_IdOrderByCreatedAtDesc(
                        merchantId
                );
    }

    @Override
    @Transactional(readOnly = true)
    public List<MerchantTransactionHistoryDTO> getTransactionHistory(UUID merchantId) {
        List<MerchantTransactionHistoryDTO> list = new java.util.ArrayList<>();

        // 1. Nguồn tiền NHẬN: Các kỳ đối soát đã xác nhận (CONFIRMED)
        List<MerchantSettlement> confirmedSettlements = settlementRepository
                .findByMerchant_IdAndStatusOrderByStartDateDesc(merchantId, SettlementStatus.CONFIRMED);

        for (MerchantSettlement s : confirmedSettlements) {
            LocalDateTime time = s.getConfirmedAt() != null ? s.getConfirmedAt() : s.getEndDate();
            list.add(MerchantTransactionHistoryDTO.builder()
                    .id("STL-" + s.getId())
                    .createdAt(time)
                    .type("RECEIVE_SETTLEMENT")
                    .typeDisplayName("Nhận tiền đối soát")
                    .typeBadgeClass("bg-success-subtle text-success border border-success-subtle")
                    .amount(s.getNetRevenue())
                    .isIncome(true)
                    .accountInfo("Cộng vào Số dư khả dụng")
                    .periodLabel("Kỳ " + s.getPeriodKey())
                    .status("COMPLETED")
                    .statusDisplayName("Đã cộng ví")
                    .statusBadgeClass("bg-success")
                    .completedAt(time)
                    .build());
        }

        // 2. Nguồn tiền RÚT: Các yêu cầu rút tiền / thanh lý
        List<MerchantPayoutRequest> requests = payoutRequestRepository
                .findByMerchant_IdOrderByCreatedAtDesc(merchantId);

        for (MerchantPayoutRequest req : requests) {
            String typeName = req.getType() == PayoutRequestType.LIQUIDATION ? "Thanh lý" : "Rút tiền";
            String typeBadge = req.getType() == PayoutRequestType.LIQUIDATION 
                    ? "bg-danger-subtle text-danger border border-danger-subtle" 
                    : "bg-primary-subtle text-primary border border-primary-subtle";

            String statusDisplay = "Chờ Admin duyệt";
            String statusBadge = "bg-warning text-dark";
            if (req.getStatus() == PayoutRequestStatus.COMPLETED) {
                statusDisplay = "Đã chuyển tiền";
                statusBadge = "bg-success";
            } else if (req.getStatus() == PayoutRequestStatus.REJECTED) {
                statusDisplay = "Bị từ chối";
                statusBadge = "bg-danger";
            }

            list.add(MerchantTransactionHistoryDTO.builder()
                    .id(req.getId().toString())
                    .createdAt(req.getCreatedAt())
                    .type(req.getType().name())
                    .typeDisplayName(typeName)
                    .typeBadgeClass(typeBadge)
                    .amount(req.getAmount())
                    .isIncome(false)
                    .accountInfo(req.getBankName() + " " + req.getBankAccountNumber())
                    .periodLabel("-")
                    .status(req.getStatus().name())
                    .statusDisplayName(statusDisplay)
                    .statusBadgeClass(statusBadge)
                    .completedAt(req.getCompletedAt())
                    .rejectedAt(req.getRejectedAt())
                    .transferProofUrl(req.getTransferProofUrl())
                    .adminNote(req.getAdminNote())
                    .build());
        }

        // Sắp xếp giảm dần theo thời gian (mới nhất lên trước)
        list.sort((a, b) -> {
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        return list;
    }


    // =====================================================
    // HELPER
    // =====================================================

    private Merchant getMerchant(
            UUID merchantId
    ) {

        return merchantRepository
                .findById(merchantId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy Merchant."
                        )
                );
    }


    private void validateMerchantCanRequest(
            Merchant merchant
    ) {

        if (merchant.getMerchantStatus()
                != MerchantStatus.APPROVED) {

            throw new IllegalStateException(
                    "Merchant hiện không ở trạng thái hoạt động."
            );
        }
    }


    private void validateBankAccount(
            Merchant merchant
    ) {

        if (merchant.getBankName() == null
                || merchant.getBankName()
                .trim()
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "Merchant chưa cập nhật tên ngân hàng."
            );
        }


        if (merchant.getBankAccountNumber() == null
                || merchant.getBankAccountNumber()
                .trim()
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "Merchant chưa cập nhật số tài khoản ngân hàng."
            );
        }
    }


    private void validateNoPendingRequest(
            UUID merchantId
    ) {

        boolean exists =
                payoutRequestRepository
                        .existsByMerchant_IdAndStatus(
                                merchantId,
                                PayoutRequestStatus.PENDING
                        );


        if (exists) {

            throw new IllegalStateException(
                    "Merchant đang có yêu cầu chờ Admin xử lý."
            );
        }
    }
}
