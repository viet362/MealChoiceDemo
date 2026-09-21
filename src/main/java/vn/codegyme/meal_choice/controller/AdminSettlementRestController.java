package vn.codegyme.meal_choice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.codegyme.meal_choice.dto.settlement.AdminSettlementItemDTO;
import vn.codegyme.meal_choice.dto.settlement.SettlementOverviewDTO;
import vn.codegyme.meal_choice.entity.MerchantSettlement;
import vn.codegyme.meal_choice.repository.MerchantSettlementRepository;
import vn.codegyme.meal_choice.service.MerchantSettlementService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/settlements")
@RequiredArgsConstructor
public class AdminSettlementRestController {

    private final MerchantSettlementService settlementService;
    private final MerchantSettlementRepository settlementRepository;

    /**
     * Lấy danh sách kỳ đối soát cho Admin
     */
    @GetMapping
    public ResponseEntity<List<AdminSettlementItemDTO>> getSettlements(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "keyword", required = false) String keyword) {
        List<AdminSettlementItemDTO> list = settlementService.getAdminSettlements(status, keyword);
        return ResponseEntity.ok(list);
    }

    /**
     * Lấy chi tiết đơn hàng và dòng tiền của kỳ đối soát để Admin tra soát
     */
    @GetMapping("/{id}/overview")
    public ResponseEntity<?> getSettlementOverview(@PathVariable("id") Long settlementId) {
        try {
            MerchantSettlement settlement = settlementRepository.findById(settlementId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy kỳ đối soát ID: " + settlementId));

            SettlementOverviewDTO overview = settlementService.getSettlementOverview(
                    settlement.getMerchant().getId(),
                    settlement.getPeriodKey(),
                    settlement.getPeriodType()
            );

            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            log.error("Lỗi lấy chi tiết đối soát Admin: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Duyệt khiếu nại (RESOLVED): Chấp thuận khiếu nại của Merchant, chuyển settlement sang CONFIRMED để chuyển tiền
     */
    @PostMapping("/claims/{claimId}/resolve")
    public ResponseEntity<?> resolveClaim(
            @PathVariable("claimId") Long claimId,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            String adminNote = null;
            BigDecimal adjustmentAmount = BigDecimal.ZERO;

            if (body != null) {
                if (body.get("adminNote") != null) {
                    adminNote = body.get("adminNote").toString();
                }
                if (body.get("adjustmentAmount") != null) {
                    try {
                        adjustmentAmount = new BigDecimal(body.get("adjustmentAmount").toString().trim());
                    } catch (Exception ignored) {}
                }
            }

            AdminSettlementItemDTO updated = settlementService.resolveClaim(claimId, adjustmentAmount, adminNote);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã phê duyệt khiếu nại thành công! Kỳ đối soát được chuyển sang trạng thái ĐÃ XÁC NHẬN để thực hiện lệnh chi trả tiền (Payout).",
                    "data", updated
            ));
        } catch (Exception e) {
            log.error("Lỗi duyệt khiếu nại ID {}: {}", claimId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Từ chối khiếu nại (REJECTED): Giải trình lý do từ chối cho Merchant, chuyển settlement về PENDING_CONFIRMATION
     */
    @PostMapping("/claims/{claimId}/reject")
    public ResponseEntity<?> rejectClaim(
            @PathVariable("claimId") Long claimId,
            @RequestBody Map<String, String> body) {
        try {
            String adminNote = body != null ? body.get("adminNote") : null;
            if (adminNote == null || adminNote.trim().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Vui lòng nhập lý do/ghi chú giải trình từ chối khiếu nại cho Merchant."
                ));
            }

            AdminSettlementItemDTO updated = settlementService.rejectClaim(claimId, adminNote);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã từ chối khiếu nại! Phản hồi đã được gửi lại cho Merchant xem xét.",
                    "data", updated
            ));
        } catch (Exception e) {
            log.error("Lỗi từ chối khiếu nại ID {}: {}", claimId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}
