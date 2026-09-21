package vn.codegyme.meal_choice.dto.merchant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

@Data
public class MerchantAddressRequest {

    @NotBlank(message = "Tỉnh/Thành phố không được để trống")
    private String provinceCode;

    @NotBlank(message = "Quận/Huyện không được để trống")
    private String districtCode;

    @NotBlank(message = "Phường/Xã không được để trống")
    private String wardCode;

    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    private String merchantAddress;

    @NotNull(message = "Giờ mở cửa không được để trống")
    private LocalTime merchantOpenTime;

    @NotNull(message = "Giờ đóng cửa không được để trống")
    private LocalTime merchantCloseTime;

    private boolean isDefault = false;
}