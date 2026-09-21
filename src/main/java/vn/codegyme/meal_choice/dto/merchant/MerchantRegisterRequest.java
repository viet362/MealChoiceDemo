package vn.codegyme.meal_choice.dto.merchant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MerchantRegisterRequest {

    @NotBlank(message = "Tên nhà hàng không được để trống")
    private String merchantRestaurantName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(
            regexp = "^[0-9]{10}$",
            message = "Số điện thoại phải gồm 10 chữ số"
    )
    private String merchantPhone;

    @NotBlank(message = "Email không được để trống")
    @Pattern(
            regexp = "^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
            message = "Email chỉ được chứa chữ, số và _ - ."
    )
    @Email(message = "Email không đúng định dạng")
    private String merchantEmail;

    @NotBlank(message = "Địa chỉ không được để trống")
    private String merchantAddress;

    @NotBlank(message = "Vui lòng chọn tỉnh/thành phố")
    private String provinceCode;

    @NotBlank(message = "Vui lòng chọn quận/huyện")
    private String districtCode;

    @NotBlank(message = "Vui lòng chọn phường/xã")
    private String wardCode;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(
            min = 6,
            message = "Mật khẩu tối thiểu 6 ký tự"
    )
    private String password;

    @NotBlank(message = "Vui lòng nhập lại mật khẩu")
    private String confirmPassword;
}