package vn.codegyme.meal_choice.dto.order;

import lombok.*;
import vn.codegyme.meal_choice.entity.OrderStatus;
import vn.codegyme.meal_choice.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponseDTO {

    private Long id;
    private String orderCode;

    private UUID merchantId;
    private String merchantName;
    private String merchantAddress;
    private String merchantPhone;
    private String merchantBankName;
    private String merchantBankAccountNumber;

    private UUID userId;
    private String customerName;
    private String customerPhone;
    private String deliveryAddress;
    private String note;

    private UUID deliveryPartnerId;
    private String deliveryPartnerName;

    private OrderStatus status;
    private String statusDisplayName;
    private String statusBadgeClass;

    private PaymentMethod paymentMethod;
    private String paymentMethodDisplayName;

    private BigDecimal subtotalPrice;
    private BigDecimal shippingFee;
    private BigDecimal serviceFee;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private Integer totalItems;

    private String cancelReason;

    private LocalDateTime acceptedAt;
    private String formattedAcceptedAt;

    private LocalDateTime preparingUntil;
    private String formattedPreparingUntil;

    private LocalDateTime estimatedDeliveryTime;
    private String formattedEstimatedDeliveryTime;

    private Long remainingPrepSeconds;
    private Long remainingDeliverySeconds;
    private Boolean canComplete;

    /**
     * true khi khách hàng còn được phép tự hủy đơn (đơn đang ở trạng thái PENDING).
     * Giao diện /user/orders dùng cờ này để bật/tắt nút "Hủy đơn".
     */
    private Boolean canCancelByUser;

    private LocalDateTime createdAt;
    private String formattedCreatedAt;

    private List<OrderItemResponseDTO> items;

    public String getContactName() {
        return customerName;
    }

    public void setContactName(String contactName) {
        this.customerName = contactName;
    }

    public String getContactPhone() {
        return customerPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.customerPhone = contactPhone;
    }

    public BigDecimal getSubtotal() {
        return subtotalPrice;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotalPrice = subtotal;
    }
}