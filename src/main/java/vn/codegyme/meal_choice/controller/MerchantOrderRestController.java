package vn.codegyme.meal_choice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import vn.codegyme.meal_choice.dto.order.CancelOrderRequestDTO;
import vn.codegyme.meal_choice.dto.order.OrderResponseDTO;
import vn.codegyme.meal_choice.entity.Merchant;
import vn.codegyme.meal_choice.entity.OrderStatus;
import vn.codegyme.meal_choice.repository.MerchantRepository;
import vn.codegyme.meal_choice.security.CustomUserDetails;
import vn.codegyme.meal_choice.service.MerchantOrderService;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/merchant/orders")
@RequiredArgsConstructor
public class MerchantOrderRestController {

    private final MerchantOrderService merchantOrderService;
    private final MerchantRepository merchantRepository;

    private Merchant getCurrentMerchant() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new RuntimeException("Người dùng chưa đăng nhập hoặc phiên làm việc không hợp lệ");
        }

        UUID userId = userDetails.getId();
        return merchantRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin Merchant của tài khoản"));
    }

    /**
     * REST API: Lấy danh sách đơn hàng của Merchant (có thể lọc theo trạng thái, phân trang 50 đơn/trang)
     */
    @GetMapping
    public ResponseEntity<?> getOrders(
            @RequestParam(name = "status", required = false) OrderStatus status,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size) {
        try {
            Merchant merchant = getCurrentMerchant();
            Pageable pageable = PageRequest.of(
                    Math.max(0, page),
                    size,
                    Sort.by(Sort.Direction.DESC, "id")
            );

            Page<OrderResponseDTO> orderPage = merchantOrderService.getMerchantOrders(
                    merchant.getId(),
                    status,
                    keyword,
                    pageable
            );

            long pendingCount = merchantOrderService.countPendingOrders(merchant.getId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orders", orderPage.getContent(),
                    "totalElements", orderPage.getTotalElements(),
                    "totalPages", orderPage.getTotalPages(),
                    "currentPage", orderPage.getNumber(),
                    "pendingCount", pendingCount
            ));
        } catch (Exception e) {
            log.error("Lỗi khi tải danh sách đơn hàng API: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * REST API: Xem chi tiết một đơn hàng của Merchant
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderDetail(@PathVariable("id") Long id) {
        try {
            Merchant merchant = getCurrentMerchant();
            OrderResponseDTO order = merchantOrderService.getMerchantOrderDetail(merchant.getId(), id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", order
            ));
        } catch (Exception e) {
            log.error("Lỗi khi xem chi tiết đơn hàng API: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * REST API: Nhận đơn hàng (Chuyển trạng thái sang PREPARING)
     */
    @PostMapping("/{id}/accept")
    public ResponseEntity<?> acceptOrder(@PathVariable("id") Long id) {
        try {
            Merchant merchant = getCurrentMerchant();
            OrderResponseDTO updatedOrder = merchantOrderService.acceptOrder(merchant.getId(), id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã nhận đơn hàng thành công!",
                    "data", updatedOrder
            ));
        } catch (Exception e) {
            log.error("Lỗi khi nhận đơn hàng API: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * REST API: Bắt đầu giao hàng (Chuyển trạng thái sang DELIVERING)
     */
    @PostMapping("/{id}/start-delivery")
    public ResponseEntity<?> startDelivery(@PathVariable("id") Long id) {
        try {
            Merchant merchant = getCurrentMerchant();
            OrderResponseDTO updatedOrder = merchantOrderService.startDelivery(merchant.getId(), id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã bắt đầu giao đơn hàng!",
                    "data", updatedOrder
            ));
        } catch (Exception e) {
            log.error("Lỗi khi bắt đầu giao hàng API: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * REST API: Hủy đơn hàng (Chuyển trạng thái sang CANCELLED)
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(
            @PathVariable("id") Long id,
            @RequestBody(required = false) CancelOrderRequestDTO request) {
        try {
            Merchant merchant = getCurrentMerchant();
            String reason = (request != null && request.getCancelReason() != null && !request.getCancelReason().isBlank())
                    ? request.getCancelReason()
                    : "Merchant hủy đơn";
            OrderResponseDTO updatedOrder = merchantOrderService.cancelOrderByMerchant(merchant.getId(), id, reason);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã hủy đơn hàng thành công!",
                    "data", updatedOrder
            ));
        } catch (Exception e) {
            log.error("Lỗi khi hủy đơn hàng API: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * REST API: Xác nhận đã nhận tiền & Hoàn thành đơn hàng (Chuyển sang COMPLETED)
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<?> completeOrder(@PathVariable("id") Long id) {
        try {
            Merchant merchant = getCurrentMerchant();
            OrderResponseDTO updatedOrder = merchantOrderService.completeOrder(merchant.getId(), id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Xác nhận đã nhận tiền và hoàn thành đơn hàng thành công!",
                    "data", updatedOrder
            ));
        } catch (Exception e) {
            log.error("Lỗi khi hoàn thành đơn hàng API: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * REST API: Xác nhận khách không nhận hàng (Chuyển sang CANCELLED)
     */
    @PostMapping("/{id}/failed-delivery")
    public ResponseEntity<?> failedDelivery(
            @PathVariable("id") Long id,
            @RequestBody(required = false) CancelOrderRequestDTO request) {
        try {
            Merchant merchant = getCurrentMerchant();
            String reason = (request != null && request.getCancelReason() != null && !request.getCancelReason().isBlank())
                    ? request.getCancelReason()
                    : "Khách không nhận hàng";
            OrderResponseDTO updatedOrder = merchantOrderService.markFailedDelivery(merchant.getId(), id, reason);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã ghi nhận khách không nhận hàng!",
                    "data", updatedOrder
            ));
        } catch (Exception e) {
            log.error("Lỗi khi ghi nhận khách không nhận hàng API: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}
