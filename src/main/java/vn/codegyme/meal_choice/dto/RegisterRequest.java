package vn.codegyme.meal_choice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email phải có định dạng hợp lệ")
    @Pattern(
            regexp = "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$",
            message = "Email phải có định dạng hợp lệ"
    )
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, max = 16, message = "Mật khẩu phải có từ 8-16 ký tự")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
            message = "Mật khẩu phải bao gồm cả chữ và số"
    )
    private String password;

    @NotBlank(message = "Mật khẩu xác nhận không được để trống")
    private String confirmPassword;

    @NotBlank(message = "Tên hiển thị không được để trống")
    @Size(min = 2, max = 30, message = "Tên hiển thị không được vượt quá 30 kí tự")
    private String displayName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(
            regexp = "^(0|\\+84)[35789][0-9]{8}$",
            message = "Số điện thoại không đúng định dạng Việt Nam"
    )
    private String phoneNumber;
}
