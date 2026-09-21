package vn.codegyme.meal_choice.dto.food;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class FoodUpdateRequest {
    @NotBlank(message = "Tên món ăn không được để trống")
    private String foodName;

    @NotNull(message = "Địa chỉ không được để trống")
    private UUID merchantAddressId;

    @NotEmpty(message = "Vui lòng chọn ít nhất một danh mục")
    private List<Long> categoryIds;

    private List<Long> tagIds = new ArrayList<>();

    private List<Long> couponIds = new ArrayList<>();

    @NotNull(message = "Thời gian chuẩn bị không được để trống")
    private Integer preparationTime;

    private String foodNote;

    @NotNull(message = "Giá món ăn không được để trống")
    @DecimalMin(
            value = "0.0",
            inclusive = false,
            message = "Giá món ăn phải lớn hơn 0"
    )
    private BigDecimal price;

    // Loại khuyến mãi
    private String discountType;

    // Mức giảm khi chọn phần trăm
    @DecimalMin(
            value = "0.0",
            inclusive = false,
            message = "Giá trị khuyến mãi phải lớn hơn 0"
    )
    private BigDecimal discountValue;

    // Giá sau khuyến mãi
    @DecimalMin(
            value = "0.0",
            inclusive = true,
            message = "Giá sau khuyến mãi không được nhỏ hơn 0"
    )
    private BigDecimal discountPrice;

    @DecimalMin(
            value = "0.0",
            inclusive = true,
            message = "Phí dịch vụ không được nhỏ hơn 0"
    )
    private BigDecimal serviceFee = BigDecimal.ZERO;
}