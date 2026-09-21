package vn.codegyme.meal_choice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import vn.codegyme.meal_choice.entity.Merchant;
import vn.codegyme.meal_choice.repository.MerchantRepository;
import vn.codegyme.meal_choice.security.CustomUserDetails;
import vn.codegyme.meal_choice.service.MerchantStatService;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/merchant/stats")
@RequiredArgsConstructor
@CrossOrigin("*")
public class MerchantStatRestController {

    private final MerchantStatService statService;
    private final MerchantRepository merchantRepository;

    private UUID resolveMerchantId(UUID requestedMerchantId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            boolean isAdmin = userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (isAdmin && requestedMerchantId != null) {
                return requestedMerchantId;
            }

            Merchant merchant = merchantRepository.findByUser_Id(userDetails.getId())
                    .orElse(null);
            if (merchant != null) {
                if (requestedMerchantId != null && !merchant.getId().equals(requestedMerchantId) && !isAdmin) {
                    throw new RuntimeException("Bạn không có quyền xem thống kê của cửa hàng khác");
                }
                return merchant.getId();
            }
        }

        if (requestedMerchantId != null) {
            return requestedMerchantId;
        }

        throw new RuntimeException("Không thể xác định thông tin Merchant");
    }

    @GetMapping("/revenue")
    public ResponseEntity<?> getRevenue(
            @RequestParam(required = false) UUID merchantId,
            @RequestParam(defaultValue = "MONTH") String type) {
        try {
            UUID targetId = resolveMerchantId(merchantId);
            return ResponseEntity.ok(statService.getRevenueStats(targetId, type));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/foods")
    public ResponseEntity<?> getFoodStats(@RequestParam(required = false) UUID merchantId) {
        try {
            UUID targetId = resolveMerchantId(merchantId);
            return ResponseEntity.ok(statService.getFoodStats(targetId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/customers")
    public ResponseEntity<?> getCustomerStats(@RequestParam(required = false) UUID merchantId) {
        try {
            UUID targetId = resolveMerchantId(merchantId);
            return ResponseEntity.ok(statService.getCustomerStats(targetId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/coupons")
    public ResponseEntity<?> getCouponStats(@RequestParam(required = false) UUID merchantId) {
        try {
            UUID targetId = resolveMerchantId(merchantId);
            return ResponseEntity.ok(statService.getCouponStats(targetId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}