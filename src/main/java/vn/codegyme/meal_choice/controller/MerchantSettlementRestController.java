package vn.codegyme.meal_choice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.codegyme.meal_choice.dto.settlement.SettlementOverviewDTO;
import vn.codegyme.meal_choice.dto.settlement.SettlementPeriodOptionDTO;
import vn.codegyme.meal_choice.entity.Merchant;
import vn.codegyme.meal_choice.entity.SettlementClaimReason;
import vn.codegyme.meal_choice.entity.User;
import vn.codegyme.meal_choice.repository.MerchantRepository;
import vn.codegyme.meal_choice.repository.UserRepository;
import vn.codegyme.meal_choice.security.CustomUserDetails;
import vn.codegyme.meal_choice.service.MerchantSettlementService;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/merchant/settlements")
@RequiredArgsConstructor
public class MerchantSettlementRestController {

    private final MerchantSettlementService settlementService;
    private final MerchantRepository merchantRepository;
    private final UserRepository userRepository;

    private Merchant getCurrentMerchant() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new RuntimeException("Người dùng chưa đăng nhập hoặc phiên làm việc không hợp lệ");
        }

        UUID userId = null;
        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            userId = userDetails.getId();
        } else {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                userId = user.getId();
            }
        }

        if (userId != null) {
            Optional<Merchant> merchantOpt = merchantRepository.findByUser_Id(userId);
            if (merchantOpt.isPresent()) {
                return merchantOpt.get();
            }
        }

        String email = authentication.getName();
        Optional<Merchant> merchantByEmail = merchantRepository.findByMerchantEmail(email);
        if (merchantByEmail.isPresent()) {
            return merchantByEmail.get();
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            List<Merchant> merchants = merchantRepository.findAll();
            if (!merchants.isEmpty()) {
                return merchants.get(0);
            }
        }

        throw new RuntimeException("Không tìm thấy thông tin Merchant của tài khoản hiện tại");
    }

    /**
     * Danh sách các kỳ đối soát có sẵn (Tuần hoặc Tháng)
     */
    @GetMapping("/periods")
    public ResponseEntity<?> getAvailablePeriods(
            @RequestParam(name = "periodType", defaultValue = "MONTH") String periodType) {
        try {
            Merchant merchant = getCurrentMerchant();
            List<SettlementPeriodOptionDTO> periods = settlementService.getAvailablePeriods(merchant.getId(), periodType);
            return ResponseEntity.ok(periods);
        } catch (Exception e) {
            log.error("Lỗi lấy danh sách kỳ đối soát: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Lấy dữ liệu tổng quan dòng tiền và danh sách đơn hàng theo kỳ
     */
    @GetMapping("/overview")
    public ResponseEntity<?> getSettlementOverview(
            @RequestParam(name = "periodKey", required = false) String periodKey,
            @RequestParam(name = "periodType", defaultValue = "MONTH") String periodType) {
        try {
            Merchant merchant = getCurrentMerchant();
            SettlementOverviewDTO overview = settlementService.getSettlementOverview(
                    merchant.getId(), periodKey, periodType);
            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            log.error("Lỗi lấy thông tin đối soát kỳ {}: {}", periodKey, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Lấy danh sách lý do khiếu nại
     */
    @GetMapping("/claim-reasons")
    public ResponseEntity<List<Map<String, String>>> getClaimReasons() {
        List<Map<String, String>> reasons = Arrays.stream(SettlementClaimReason.values())
                .map(r -> Map.of("key", r.name(), "label", r.getDisplayName()))
                .toList();
        return ResponseEntity.ok(reasons);
    }

    /**
     * Xác nhận kỳ đối soát (Confirm)
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<?> confirmSettlement(@PathVariable("id") Long settlementId) {
        try {
            Merchant merchant = getCurrentMerchant();
            SettlementOverviewDTO updated = settlementService.confirmSettlement(merchant.getId(), settlementId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Xác nhận đối soát thành công! Hệ thống đã ghi nhận và chuyển lệnh sang Admin.",
                    "data", updated
            ));
        } catch (Exception e) {
            log.error("Lỗi xác nhận kỳ đối soát {}: {}", settlementId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Khiếu nại kỳ đối soát (Claim / Dispute)
     */
    @PostMapping(value = "/{id}/claim", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> claimSettlement(
            @PathVariable("id") Long settlementId,
            @RequestParam("reason") SettlementClaimReason reason,
            @RequestParam("description") String description,
            @RequestParam(value = "evidenceImage", required = false) MultipartFile evidenceImage) {
        try {
            Merchant merchant = getCurrentMerchant();
            SettlementOverviewDTO updated = settlementService.claimSettlement(
                    merchant.getId(), settlementId, reason, description, evidenceImage);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Gửi khiếu nại đối soát thành công! Yêu cầu của bạn đã được chuyển đến Admin để xử lý.",
                    "data", updated
            ));
        } catch (Exception e) {
            log.error("Lỗi khiếu nại kỳ đối soát {}: {}", settlementId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}
