package vn.codegyme.meal_choice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.codegyme.meal_choice.entity.MerchantPayoutRequest;
import vn.codegyme.meal_choice.entity.PayoutRequestStatus;
import vn.codegyme.meal_choice.repository.MerchantPayoutRequestRepository;
import vn.codegyme.meal_choice.service.AdminPayoutService;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/payout-requests")
@RequiredArgsConstructor
public class AdminPayoutController {

        private final MerchantPayoutRequestRepository payoutRequestRepository;

        private final AdminPayoutService adminPayoutService;

        private final vn.codegyme.meal_choice.service.FileStorageService fileStorageService;

        // =====================================================
        // DANH SÁCH YÊU CẦU
        // =====================================================

        @GetMapping
        public String list(
                        @RequestParam(required = false) PayoutRequestStatus status,
                        Model model) {

                List<MerchantPayoutRequest> requests;

                if (status == null) {

                        requests = payoutRequestRepository
                                        .findAllByOrderByCreatedAtDesc();

                } else {

                        requests = payoutRequestRepository
                                        .findByStatusOrderByCreatedAtDesc(
                                                        status);
                }

                model.addAttribute(
                                "requests",
                                requests);

                model.addAttribute(
                                "selectedStatus",
                                status);

                return "admin-payout/list";
        }

        // =====================================================
        // CHI TIẾT YÊU CẦU
        // =====================================================

        @GetMapping("/{id}")
        public String detail(
                        @PathVariable UUID id,
                        Model model) {

                MerchantPayoutRequest request = payoutRequestRepository
                                .findById(id)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Không tìm thấy yêu cầu."));

                model.addAttribute(
                                "request",
                                request);

                return "admin-payout/detail";
        }

        // =====================================================
        // ADMIN HOÀN TẤT YÊU CẦU
        // =====================================================

        @PostMapping("/{id}/complete")
        public String complete(
                        @PathVariable UUID id,

                        @RequestParam(value = "proofImage", required = false) MultipartFile proofImage,

                        @RequestParam(value = "transferProofUrl", required = false) String transferProofUrl,

                        @RequestParam(required = false) String adminNote,

                        RedirectAttributes redirectAttributes) {

                try {

                        String finalProofUrl = transferProofUrl;

                        if (proofImage != null && !proofImage.isEmpty()) {
                                finalProofUrl = fileStorageService.savePayoutProofImage(proofImage);
                        }

                        if (finalProofUrl == null || finalProofUrl.trim().isEmpty()) {
                                throw new IllegalArgumentException("Vui lòng tải lên ảnh chứng từ chuyển khoản.");
                        }

                        adminPayoutService
                                        .completePayoutRequest(
                                                        id,
                                                        finalProofUrl,
                                                        adminNote);

                        redirectAttributes
                                        .addFlashAttribute(
                                                        "message",
                                                        "Đã hoàn tất yêu cầu và xác nhận chuyển khoản thành công.");

                } catch (Exception e) {

                        redirectAttributes
                                        .addFlashAttribute(
                                                        "error",
                                                        e.getMessage());
                }

                return "redirect:/admin/payout-requests/" + id;
        }

        // =====================================================
        // ADMIN TỪ CHỐI YÊU CẦU
        // =====================================================

        @PostMapping("/{id}/reject")
        public String reject(
                        @PathVariable UUID id,

                        @RequestParam String reason,

                        RedirectAttributes redirectAttributes) {

                try {

                        adminPayoutService
                                        .rejectPayoutRequest(
                                                        id,
                                                        reason);

                        redirectAttributes
                                        .addFlashAttribute(
                                                        "message",
                                                        "Đã từ chối yêu cầu.");

                } catch (Exception e) {

                        redirectAttributes
                                        .addFlashAttribute(
                                                        "error",
                                                        e.getMessage());
                }

                return "redirect:/admin/payout-requests/" + id;
        }
}
