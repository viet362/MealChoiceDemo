package vn.codegyme.meal_choice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import vn.codegyme.meal_choice.dto.order.CheckoutRequestDTO;
import vn.codegyme.meal_choice.dto.order.OrderResponseDTO;
import vn.codegyme.meal_choice.entity.Coupon;
import vn.codegyme.meal_choice.entity.DiscountType;
import vn.codegyme.meal_choice.entity.Food;
import vn.codegyme.meal_choice.entity.User;
import vn.codegyme.meal_choice.repository.CouponRepository;
import vn.codegyme.meal_choice.repository.UserRepository;
import vn.codegyme.meal_choice.security.CustomUserDetails;
import vn.codegyme.meal_choice.service.UserOrderService;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutRestController {

    private final UserOrderService userOrderService;
    private final UserRepository userRepository;
    private final CouponRepository couponRepository;

    /**
     * REST API: Lấy danh sách Coupon của Merchant áp dụng cho các món ăn trong giỏ hàng
     * GET /api/checkout/applicable-coupons?merchantId=...&foodIds=1,2,3
     */
    @GetMapping("/applicable-coupons")
    public ResponseEntity<?> getApplicableCoupons(
            @RequestParam("merchantId") UUID merchantId,
            @RequestParam(value = "foodIds", required = false) List<Long> foodIds
    ) {
        List<Coupon> activeCoupons = couponRepository.findAllActiveByMerchantId(merchantId);
        List<Map<String, Object>> results = new ArrayList<>();

        Set<Long> targetFoodIds = (foodIds != null) ? new HashSet<>(foodIds) : Collections.emptySet();

        for (Coupon c : activeCoupons) {
            List<Food> foods = c.getFoods();
            List<Long> matchedFoodIds = new ArrayList<>();
            List<String> matchedFoodNames = new ArrayList<>();

            if (foods != null && !foods.isEmpty()) {
                for (Food f : foods) {
                    if (targetFoodIds.contains(f.getId())) {
                        matchedFoodIds.add(f.getId());
                        matchedFoodNames.add(f.getFoodName());
                    }
                }
                // Nếu coupon gắn với món cụ thể mà giỏ không có món nào thuộc danh sách -> bỏ qua
                if (matchedFoodIds.isEmpty()) {
                    continue;
                }
            } else {
                matchedFoodNames.add("Tất cả món của quán");
            }

            String discountDesc = (c.getDiscountType() == DiscountType.PERCENT)
                    ? "Giảm " + c.getDiscountValue().stripTrailingZeros().toPlainString() + "%"
                    : "Giảm " + String.format("%,d", c.getDiscountValue().longValue()).replace(',', '.') + "đ";

            String applyForText = String.join(", ", matchedFoodNames);
            String displayText = c.getCouponCode() + " - " + discountDesc + " (Áp dụng: " + applyForText + ")";

            Map<String, Object> item = new HashMap<>();
            item.put("id", c.getId());
            item.put("couponCode", c.getCouponCode());
            item.put("discountType", c.getDiscountType().name());
            item.put("discountValue", c.getDiscountValue());
            item.put("label", discountDesc);
            item.put("applicableFoodIds", matchedFoodIds);
            item.put("applicableFoodNames", matchedFoodNames);
            item.put("displayText", displayText);

            results.add(item);
        }

        return ResponseEntity.ok(results);
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return null;
        }

        UUID userId = userDetails.getId();
        return userRepository.findById(userId).orElse(null);
    }

    /**
     * REST API: ĐẶT HÀNG / THANH TOÁN
     */
    @PostMapping("/place-order")
    public ResponseEntity<?> placeOrder(@Valid @RequestBody CheckoutRequestDTO request) {
        try {
            User user = getAuthenticatedUser();
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "message", "Vui lòng đăng nhập trước khi thanh toán"
                ));
            }

            OrderResponseDTO response = userOrderService.placeOrder(user.getId(), request);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đặt hàng thành công!",
                    "orderCode", response.getOrderCode(),
                    "orderId", response.getId(),
                    "data", response
            ));
        } catch (Exception e) {
            log.error("Lỗi khi xử lý đặt hàng API: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}
