package vn.codegyme.meal_choice.dto.cart;

import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {

    private Long id;

    private UUID merchantId;
    private String merchantName;
    private String merchantBankName;
    private String merchantBankAccountNumber;

    @Builder.Default
    private List<CartItemResponse> items = new ArrayList<>();

    /**
     * Tổng số phần món (cộng dồn quantity).
     */
    @Builder.Default
    private Integer totalItems = 0;

    /**
     * Tổng tiền theo giá gốc, dùng để hiển thị phần "tiết kiệm".
     */
    @Builder.Default
    private BigDecimal originalSubtotal = BigDecimal.ZERO;

    /**
     * Tổng tiền món theo giá thực tế.
     */
    @Builder.Default
    private BigDecimal subtotalPrice = BigDecimal.ZERO;

    /**
     * originalSubtotal - subtotalPrice
     */
    @Builder.Default
    private BigDecimal savings = BigDecimal.ZERO;

    /**
     * Phí dịch vụ cao nhất giữa các món (khớp với cách tính trong UserOrderServiceImpl).
     */
    @Builder.Default
    private BigDecimal serviceFee = BigDecimal.ZERO;

    /**
     * subtotalPrice + serviceFee. Chưa gồm phí vận chuyển và voucher.
     */
    @Builder.Default
    private BigDecimal estimatedTotal = BigDecimal.ZERO;

    /**
     * true khi có ít nhất một món không còn phục vụ.
     */
    @Builder.Default
    private Boolean hasUnavailableItems = false;

    @Builder.Default
    private Boolean empty = true;
}