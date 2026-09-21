package vn.codegyme.meal_choice.dto.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCartItemRequest {

    /**
     * Số lượng mới. Gửi 0 để xóa món khỏi giỏ hàng.
     * Để null nếu chỉ muốn sửa ghi chú.
     */
    @Min(value = 0, message = "Số lượng không được nhỏ hơn 0")
    @Max(value = 99, message = "Số lượng tối đa cho mỗi món là 99")
    private Integer quantity;

    /**
     * Cộng/trừ số lượng hiện tại (ví dụ +1 hoặc -1).
     * Chỉ dùng khi quantity là null.
     */
    private Integer quantityDelta;

    /**
     * Ghi chú mới. Để null nếu không muốn thay đổi ghi chú.
     */
    private String note;
}