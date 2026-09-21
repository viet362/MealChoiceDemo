package vn.codegyme.meal_choice.exception;

import lombok.Getter;

import java.util.UUID;

/**
 * Ném ra khi người dùng thêm món của quán B trong lúc giỏ hàng đang chứa món của quán A.
 *
 * Giao diện bắt lỗi này để hỏi người dùng có muốn xóa giỏ hàng cũ hay không,
 * rồi gọi lại API với replaceCart = true.
 */
@Getter
public class CartMerchantConflictException extends RuntimeException {

    private final UUID currentMerchantId;
    private final String currentMerchantName;
    private final UUID newMerchantId;
    private final String newMerchantName;

    public CartMerchantConflictException(
            UUID currentMerchantId,
            String currentMerchantName,
            UUID newMerchantId,
            String newMerchantName) {

        super("Giỏ hàng đang có món của quán \""
                + currentMerchantName
                + "\". Mỗi đơn hàng chỉ đặt được từ một quán.");

        this.currentMerchantId = currentMerchantId;
        this.currentMerchantName = currentMerchantName;
        this.newMerchantId = newMerchantId;
        this.newMerchantName = newMerchantName;
    }
}