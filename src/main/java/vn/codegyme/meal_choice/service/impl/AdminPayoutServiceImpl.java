package vn.codegyme.meal_choice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegyme.meal_choice.entity.*;
import vn.codegyme.meal_choice.repository.MerchantPayoutRequestRepository;
import vn.codegyme.meal_choice.repository.MerchantRepository;
import vn.codegyme.meal_choice.service.AdminPayoutService;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminPayoutServiceImpl
                implements AdminPayoutService {

        private final MerchantPayoutRequestRepository payoutRequestRepository;

        private final MerchantRepository merchantRepository;

        // ==========================================
        // ADMIN HOÀN TẤT YÊU CẦU
        // ==========================================

        @Override
        public void completePayoutRequest(
                        UUID requestId,
                        String transferProofUrl,
                        String adminNote) {

                MerchantPayoutRequest request = payoutRequestRepository
                                .findById(requestId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Không tìm thấy yêu cầu."));

                if (request.getStatus() != PayoutRequestStatus.PENDING) {

                        throw new IllegalStateException(
                                        "Yêu cầu này đã được xử lý.");
                }

                if (transferProofUrl == null
                                || transferProofUrl.trim().isEmpty()) {

                        throw new IllegalArgumentException(
                                        "Vui lòng upload ảnh chuyển khoản.");
                }

                request.setTransferProofUrl(
                                transferProofUrl);

                request.setAdminNote(
                                adminNote);

                request.setStatus(
                                PayoutRequestStatus.COMPLETED);

                request.setCompletedAt(
                                LocalDateTime.now());

                // Nếu là THANH LÝ -> BLOCK Merchant
                if (request.getType() == PayoutRequestType.LIQUIDATION) {

                        Merchant merchant = request.getMerchant();

                        merchant.setMerchantStatus(
                                        MerchantStatus.BLOCKED);

                        merchant.setLockReason(
                                        "Merchant đã thanh lý hợp đồng.");

                        merchant.setLockedAt(
                                        LocalDateTime.now());

                        merchantRepository.save(
                                        merchant);
                }

                payoutRequestRepository.save(
                                request);
        }

        // ==========================================
        // ADMIN TỪ CHỐI YÊU CẦU
        // ==========================================

        @Override
        public void rejectPayoutRequest(
                        UUID requestId,
                        String reason) {

                MerchantPayoutRequest request = payoutRequestRepository
                                .findById(requestId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Không tìm thấy yêu cầu."));

                if (request.getStatus() != PayoutRequestStatus.PENDING) {

                        throw new IllegalStateException(
                                        "Yêu cầu này đã được xử lý.");
                }

                if (reason == null
                                || reason.trim().isEmpty()) {

                        throw new IllegalArgumentException(
                                        "Vui lòng nhập lý do từ chối.");
                }

                request.setStatus(
                                PayoutRequestStatus.REJECTED);

                request.setAdminNote(
                                reason.trim());

                request.setRejectedAt(
                                LocalDateTime.now());

                payoutRequestRepository.save(
                                request);
        }
}
