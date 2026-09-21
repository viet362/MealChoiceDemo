package vn.codegyme.meal_choice.dto.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddToCartRequest {

    @NotNull(message = "Mã món ăn không được để trống")
    private Long foodId;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn hoặc bằng 1")
    @Max(value = 99, message = "Số lượng tối đa cho mỗi món là 99")
    @Builder.Default
    private Integer quantity = 1;

    private String note;

    /**
     * Khi giỏ hàng đang có món của quán khác:
     * - false (mặc định): API trả về 409 kèm mã lỗi MERCHANT_CONFLICT để giao diện hỏi lại người dùng.
     * - true: xóa sạch giỏ hàng cũ rồi thêm món mới.
     */
    @Builder.Default
    private Boolean replaceCart = false;
}