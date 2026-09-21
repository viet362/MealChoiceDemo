package vn.codegyme.meal_choice.entity;

import lombok.Getter;

@Getter
public enum SettlementClaimReason {
    COMMISSION_FEE_MISMATCH("Sai lệch phí chiết khấu sàn"),
    WRONG_COMMISSION("Sai lệch phí chiết khấu sàn"),
    MISSING_ORDERS("Thiếu đơn hàng trong kỳ đối soát"),
    DISCOUNT_MISMATCH("Sai lệch số tiền khuyến mãi"),
    CALCULATION_ERROR("Sai lệch số tiền thực nhận"),
    OTHER("Lý do khác");

    private final String displayName;

    SettlementClaimReason(String displayName) {
        this.displayName = displayName;
    }
}
