package vn.codegyme.meal_choice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import vn.codegyme.meal_choice.dto.cart.AddToCartRequest;
import vn.codegyme.meal_choice.dto.cart.CartResponse;
import vn.codegyme.meal_choice.dto.cart.UpdateCartItemRequest;
import vn.codegyme.meal_choice.exception.CartMerchantConflictException;
import vn.codegyme.meal_choice.security.CustomUserDetails;
import vn.codegyme.meal_choice.service.CartService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * API giỏ hàng của Khách hàng.
 *
 * GET    /api/cart                  Xem giỏ hàng
 * POST   /api/cart/items            Thêm món vào giỏ
 * PATCH  /api/cart/items/{itemId}   Sửa số lượng hoặc ghi chú
 * DELETE /api/cart/items/{itemId}   Xóa một món
 * DELETE /api/cart                  Xóa toàn bộ giỏ hàng
 */
@Slf4j
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartRestController {

    private final CartService cartService;

    // ==================== XEM GIỎ HÀNG ====================

    @GetMapping
    public ResponseEntity<?> getMyCart() {
        try {
            UUID userId = requireUserId();
            CartResponse cart = cartService.getMyCart(userId);

            return ResponseEntity.ok(success("Lấy giỏ hàng thành công", cart));

        } catch (Exception e) {
            return handleError(e, "Không tải được giỏ hàng");
        }
    }

    // ==================== THÊM MÓN ====================

    @PostMapping("/items")
    public ResponseEntity<?> addItem(@Valid @RequestBody AddToCartRequest request) {
        try {
            UUID userId = requireUserId();
            CartResponse cart = cartService.addItem(userId, request);

            return ResponseEntity.ok(success("Đã thêm món vào giỏ hàng", cart));

        } catch (CartMerchantConflictException e) {
            // 409: giao diện sẽ hỏi người dùng rồi gọi lại với replaceCart = true
            Map<String, Object> body = new HashMap<>();
            body.put("success", false);
            body.put("error", "MERCHANT_CONFLICT");
            body.put("message", e.getMessage());
            body.put("currentMerchantId", e.getCurrentMerchantId());
            body.put("currentMerchantName", e.getCurrentMerchantName());
            body.put("newMerchantId", e.getNewMerchantId());
            body.put("newMerchantName", e.getNewMerchantName());

            return ResponseEntity.status(HttpStatus.CONFLICT).body(body);

        } catch (Exception e) {
            return handleError(e, "Không thêm được món vào giỏ hàng");
        }
    }

    // ==================== SỬA MÓN ====================

    @PatchMapping("/items/{itemId}")
    public ResponseEntity<?> updateItem(
            @PathVariable("itemId") Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        try {
            UUID userId = requireUserId();
            CartResponse cart = cartService.updateItem(userId, itemId, request);

            return ResponseEntity.ok(success("Đã cập nhật giỏ hàng", cart));

        } catch (Exception e) {
            return handleError(e, "Không cập nhật được giỏ hàng");
        }
    }

    // ==================== XÓA MỘT MÓN ====================

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<?> removeItem(@PathVariable("itemId") Long itemId) {
        try {
            UUID userId = requireUserId();
            CartResponse cart = cartService.removeItem(userId, itemId);

            return ResponseEntity.ok(success("Đã xóa món khỏi giỏ hàng", cart));

        } catch (Exception e) {
            return handleError(e, "Không xóa được món khỏi giỏ hàng");
        }
    }

    // ==================== XÓA TOÀN BỘ GIỎ ====================

    @DeleteMapping
    public ResponseEntity<?> clearCart() {
        try {
            UUID userId = requireUserId();
            CartResponse cart = cartService.clearCart(userId);

            return ResponseEntity.ok(success("Đã xóa toàn bộ giỏ hàng", cart));

        } catch (Exception e) {
            return handleError(e, "Không xóa được giỏ hàng");
        }
    }

    // ==================== HELPER ====================

    private UUID requireUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new UnauthenticatedException();
        }

        return userDetails.getId();
    }

    private Map<String, Object> success(String message, CartResponse cart) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("message", message);
        body.put("data", cart);
        return body;
    }

    private ResponseEntity<?> handleError(Exception e, String fallbackMessage) {

        if (e instanceof UnauthenticatedException) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "Vui lòng đăng nhập để sử dụng giỏ hàng"
            ));
        }

        log.warn("Lỗi xử lý giỏ hàng: {}", e.getMessage());

        String message = (e.getMessage() != null && !e.getMessage().isBlank())
                ? e.getMessage()
                : fallbackMessage;

        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", message
        ));
    }

    /**
     * Đánh dấu request chưa đăng nhập để trả về đúng mã 401.
     */
    private static class UnauthenticatedException extends RuntimeException {
        UnauthenticatedException() {
            super("Chưa đăng nhập");
        }
    }
}