package vn.codegyme.meal_choice.entity;

import lombok.Getter;

@Getter
public enum OrderStatus {
    PENDING("Chờ nhận hàng", "bg-warning text-dark"),
    PREPARING("Đang chuẩn bị", "bg-primary text-white"),
    DELIVERING("Đang giao", "bg-info text-dark"),
    COMPLETED("Hoàn thành", "bg-success text-white"),
    CANCELLED("Đã hủy", "bg-danger text-white");

    private final String displayName;
    private final String badgeClass;

    OrderStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }
}
