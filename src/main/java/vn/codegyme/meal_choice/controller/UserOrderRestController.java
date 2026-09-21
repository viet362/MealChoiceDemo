package vn.codegyme.meal_choice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import vn.codegyme.meal_choice.dto.order.CancelOrderRequestDTO;
import vn.codegyme.meal_choice.dto.order.OrderResponseDTO;
import vn.codegyme.meal_choice.entity.OrderStatus;
import vn.codegyme.meal_choice.security.CustomUserDetails;
import vn.codegyme.meal_choice.service.UserOrderService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * API đơn hàng của Khách hàng.
 *
 * GET  /api/user/orders             Danh sách đơn hàng của tôi
 * GET  /api/user/orders/{id}        Chi tiết một đơn hàng của tôi
 * POST /api/user/orders/{id}/cancel Hủy đơn hàng (chỉ khi quán chưa nhận đơn)
 */
@Slf4j
@RestController
@RequestMapping("/api/user/orders")
@RequiredArgsConstructor
public class UserOrderRestController {

    private final UserOrderService userOrderService;

    // ==================== DANH SÁCH ĐƠN HÀNG ====================

    @GetMapping
    public ResponseEntity<?> getMyOrders(
            @RequestParam(name = "status", required = false) OrderStatus status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        try {
            UUID userId = requireUserId();

            Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));

            Page<OrderResponseDTO> orderPage =
                    userOrderService.getUserOrders(userId, status, pageable);

            Map<String, Object> body = new HashMap<>();
            body.put("success", true);
            body.put("orders", orderPage.getContent());
            body.put("totalElements", orderPage.getTotalElements());
            body.put("totalPages", orderPage.getTotalPages());
            body.put("currentPage", orderPage.getNumber());

            return ResponseEntity.ok(body);

        } catch (Exception e) {
            return handleError(e, "Không tải được danh sách đơn hàng");
        }
    }

    // ==================== CHI TIẾT ĐƠN HÀNG ====================

    @GetMapping("/{id}")
    public ResponseEntity<?> getMyOrderDetail(@PathVariable("id") Long id) {

        try {
            UUID userId = requireUserId();

            OrderResponseDTO order = userOrderService.getUserOrderDetail(userId, id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", order
            ));

        } catch (Exception e) {
            return handleError(e, "Không tải được chi tiết đơn hàng");
        }
    }

    // ==================== HỦY ĐƠN HÀNG ====================

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelMyOrder(
            @PathVariable("id") Long id,
            @RequestBody(required = false) CancelOrderRequestDTO request) {

        try {
            UUID userId = requireUserId();

            String reason = (request != null) ? request.getCancelReason() : null;

            OrderResponseDTO order = userOrderService.cancelOrderByUser(userId, id, reason);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã hủy đơn hàng " + order.getOrderCode(),
                    "data", order
            ));

        } catch (IllegalStateException e) {
            // Quán đã nhận đơn -> 409 để giao diện phân biệt với lỗi nhập liệu
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "success", false,
                    "error", "ORDER_NOT_CANCELLABLE",
                    "message", e.getMessage()
            ));

        } catch (Exception e) {
            return handleError(e, "Không hủy được đơn hàng");
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

    private ResponseEntity<?> handleError(Exception e, String fallbackMessage) {

        if (e instanceof UnauthenticatedException) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "Vui lòng đăng nhập để xem đơn hàng"
            ));
        }

        log.warn("Lỗi xử lý đơn hàng của khách: {}", e.getMessage());

        String message = (e.getMessage() != null && !e.getMessage().isBlank())
                ? e.getMessage()
                : fallbackMessage;

        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", message
        ));
    }

    private static class UnauthenticatedException extends RuntimeException {
        UnauthenticatedException() {
            super("Chưa đăng nhập");
        }
    }
}