package vn.codegyme.meal_choice.dto.coupon;

import jakarta.validation.constraints.*;
import lombok.Data;
import vn.codegyme.meal_choice.entity.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponCreateRequest {

    @NotBlank(message = "Mã coupon không được để trống")
    @Size(max = 50, message = "Mã coupon không được quá 50 ký tự")
    private String couponCode;

    @NotNull(message = "Vui lòng chọn loại giảm giá")
    private DiscountType discountType;

    @NotNull(message = "Giá trị giảm giá không được để trống")
    @DecimalMin(value = "0.01", message = "Giá trị giảm giá phải lớn hơn 0")
    private BigDecimal discountValue;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    @Min(value = 1, message = "Số lượt sử dụng phải lớn hơn 0")
    private Integer usageLimit;
}