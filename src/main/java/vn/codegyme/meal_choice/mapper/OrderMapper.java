package vn.codegyme.meal_choice.mapper;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import vn.codegyme.meal_choice.dto.order.OrderItemResponseDTO;
import vn.codegyme.meal_choice.dto.order.OrderResponseDTO;
import vn.codegyme.meal_choice.entity.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lớp Mapper chuyên chuyển đổi Entity (Order, OrderItem) sang DTO (OrderResponseDTO, OrderItemResponseDTO)
 * Được thiết kế theo chuẩn Clean Code: phân tách rõ ràng, an toàn với null và cực kỳ dễ đọc.
 */
@Component
public class OrderMapper {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    /**
     * Chuyển đổi một Entity Order sang OrderResponseDTO
     */
    public OrderResponseDTO toOrderResponseDTO(Order order) {
        if (order == null) {
            return null;
        }

        Merchant merchant = order.getMerchant();
        User user = order.getUser();
        LocalDateTime now = LocalDateTime.now();

        Long remainingPrepSeconds = null;
        if (order.getPreparingUntil() != null) {
            remainingPrepSeconds = Math.max(0L, Duration.between(now, order.getPreparingUntil()).getSeconds());
        }

        Long remainingDeliverySeconds = null;
        if (order.getEstimatedDeliveryTime() != null) {
            remainingDeliverySeconds = Math.max(0L, Duration.between(now, order.getEstimatedDeliveryTime()).getSeconds());
        }

        boolean canComplete = false;
        if (order.getStatus() == OrderStatus.DELIVERING) {
            canComplete = (remainingDeliverySeconds != null && remainingDeliverySeconds == 0L)
                    || order.getEstimatedDeliveryTime() == null
                    || now.isAfter(order.getEstimatedDeliveryTime());
        }

        return OrderResponseDTO.builder()
                // 1. Thông tin chung đơn hàng
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .createdAt(order.getCreatedAt())
                .formattedCreatedAt(formatDateTime(order.getCreatedAt()))
                .acceptedAt(order.getAcceptedAt())
                .formattedAcceptedAt(formatDateTime(order.getAcceptedAt()))
                .preparingUntil(order.getPreparingUntil())
                .formattedPreparingUntil(formatDateTime(order.getPreparingUntil()))
                .estimatedDeliveryTime(order.getEstimatedDeliveryTime())
                .formattedEstimatedDeliveryTime(formatDateTime(order.getEstimatedDeliveryTime()))
                .remainingPrepSeconds(remainingPrepSeconds)
                .remainingDeliverySeconds(remainingDeliverySeconds)
                .canComplete(canComplete)
                .cancelReason(order.getCancelReason())

                // 2. Trạng thái & Phương thức thanh toán
                .status(order.getStatus())
                .statusDisplayName(getStatusDisplayName(order.getStatus()))
                .statusBadgeClass(getStatusBadgeClass(order.getStatus()))
                .paymentMethod(order.getPaymentMethod())
                .paymentMethodDisplayName(getPaymentMethodDisplayName(order.getPaymentMethod()))

                // 3. Thông tin Cửa hàng (Merchant)
                .merchantId(merchant != null ? merchant.getId() : null)
                .merchantName(merchant != null ? merchant.getMerchantRestaurantName() : "")
                .merchantPhone(merchant != null ? merchant.getMerchantPhone() : "")
                .merchantAddress(getMerchantAddress(merchant))
                .merchantBankName(merchant != null ? merchant.getBankName() : "")
                .merchantBankAccountNumber(merchant != null ? merchant.getBankAccountNumber() : "")

                // 4. Thông tin Khách hàng (Customer) & Đơn vị vận chuyển
                .userId(user != null ? user.getId() : null)
                .customerName(order.getContactName())
                .customerPhone(order.getContactPhone())
                .deliveryAddress(order.getDeliveryAddress())
                .note(order.getNote())
                .deliveryPartnerId(order.getDeliveryPartner() != null ? order.getDeliveryPartner().getId() : null)
                .deliveryPartnerName(order.getDeliveryPartner() != null ? order.getDeliveryPartner().getPartnerName() : "Giao hàng tiêu chuẩn")

                // 5. Chi tiết bảng giá thanh toán
                .subtotalPrice(order.getSubtotalPrice())
                .shippingFee(order.getShippingFee())
                .serviceFee(order.getServiceFee())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .totalItems(calculateTotalQuantity(order.getOrderItems()))

                // 6. Danh sách món ăn đã đặt
                .items(toOrderItemResponseDTOList(order.getOrderItems()))
                .build();
    }

    /**
     * Chuyển đổi một Entity OrderItem sang OrderItemResponseDTO
     */
    public OrderItemResponseDTO toOrderItemResponseDTO(OrderItem item) {
        if (item == null) {
            return null;
        }

        Food food = item.getFood();

        return OrderItemResponseDTO.builder()
                .id(item.getId())
                .foodId(food != null ? food.getId() : null)
                .foodName(item.getFoodName())
                .foodImage(resolveFoodImage(item))
                .price(item.getPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .note(item.getNote())
                .build();
    }

    /**
     * Chuyển đổi danh sách OrderItem sang danh sách DTO
     */
    public List<OrderItemResponseDTO> toOrderItemResponseDTOList(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream()
                .map(this::toOrderItemResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Chuyển đổi danh sách Order sang danh sách DTO
     */
    public List<OrderResponseDTO> toOrderResponseDTOList(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }
        return orders.stream()
                .map(this::toOrderResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Chuyển đổi Page<Order> sang Page<OrderResponseDTO>
     */
    public Page<OrderResponseDTO> toOrderResponseDTOPage(Page<Order> orderPage) {
        if (orderPage == null) {
            return Page.empty();
        }
        return orderPage.map(this::toOrderResponseDTO);
    }

    // =========================================================================
    // CÁC HÀM TIỆN ÍCH PHỤ TRỢ (HELPER METHODS - GIÚP CODE TRONG SÁNG & GỌN GÀNG)
    // =========================================================================

    /**
     * Format ngày giờ hoặc trả về rỗng nếu null
     */
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : "";
    }

    /**
     * Lấy tên hiển thị trạng thái đơn hàng an toàn
     */
    private String getStatusDisplayName(OrderStatus status) {
        return status != null ? status.getDisplayName() : "";
    }

    /**
     * Lấy class badge màu sắc trạng thái an toàn
     */
    private String getStatusBadgeClass(OrderStatus status) {
        return status != null ? status.getBadgeClass() : "";
    }

    /**
     * Lấy tên hiển thị phương thức thanh toán an toàn
     */
    private String getPaymentMethodDisplayName(PaymentMethod paymentMethod) {
        return paymentMethod != null ? paymentMethod.getDisplayName() : "";
    }

    /**
     * Tính tổng số lượng món ăn trong đơn
     */
    private int calculateTotalQuantity(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        return items.stream()
                .mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                .sum();
    }

    /**
     * Lấy địa chỉ cửa hàng an toàn
     */
    private String getMerchantAddress(Merchant merchant) {
        if (merchant == null || merchant.getAddresses() == null || merchant.getAddresses().isEmpty()) {
            return "";
        }
        try {
            MerchantAddress address = merchant.getAddresses().get(0);
            if (address == null || address.getMerchantAddress() == null) {
                return "";
            }
            return address.getMerchantAddress();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Tìm ảnh đại diện của món ăn (ưu tiên ảnh lưu trong item, fallback sang ảnh của Food)
     */
    private String resolveFoodImage(OrderItem item) {
        if (item == null) {
            return "";
        }
        if (item.getFoodImage() != null && !item.getFoodImage().isBlank()) {
            return item.getFoodImage();
        }
        try {
            Food food = item.getFood();
            if (food != null && food.getImages() != null && !food.getImages().isEmpty()) {
                return food.getImages().get(0).getImageUrl();
            }
        } catch (Exception ignored) {
        }
        return "";
    }
}