package vn.codegyme.meal_choice.entity;

import lombok.Getter;

@Getter
public enum PaymentMethod {
    COD("Thanh toán khi nhận hàng (COD)"),
    CARD("Thẻ tín dụng / Ghi nợ / Chuyển khoản");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }
}
