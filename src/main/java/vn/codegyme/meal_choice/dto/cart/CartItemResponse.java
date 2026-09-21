package vn.codegyme.meal_choice.dto.cart;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponse {

    private Long id;

    private Long foodId;
    private String foodName;
    private String foodImage;

    /** Merchant sở hữu món — dùng để nhóm giỏ hàng đa quán */
    private UUID merchantId;
    private String merchantName;

    /**
     * Giá gốc niêm yết của món.
     */
    private BigDecimal price;

    /**
     * Giá khuyến mãi (có thể null).
     */
    private BigDecimal discountPrice;

    /**
     * Giá thực tế dùng để tính tiền = discountPrice nếu có, ngược lại là price.
     */
    private BigDecimal effectivePrice;

    private BigDecimal serviceFee;

    private Integer quantity;

    /**
     * effectivePrice * quantity
     */
    private BigDecimal subtotal;

    private String note;

    private Integer preparationTime;

    /**
     * false khi món đã bị merchant ẩn hoặc xóa. Giao diện sẽ hiển thị cảnh báo
     * và chặn thanh toán cho tới khi người dùng bỏ món này ra khỏi giỏ.
     */
    private Boolean available;

    private String unavailableReason;
}