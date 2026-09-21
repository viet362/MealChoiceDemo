package vn.codegyme.meal_choice.dto.merchant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

@Data
public class MerchantUpdateRequest {

    public interface ProfileUpdate {}
    public interface AddressUpdate {}

    @NotBlank(groups = ProfileUpdate.class, message = "Tên nhà hàng không được để trống")
    private String merchantRestaurantName;

    private String bankName;

    private String bankAccountNumber;

    @NotBlank(groups = AddressUpdate.class, message = "Địa chỉ không được để trống")
    private String merchantAddress;

    @NotBlank(groups = AddressUpdate.class, message = "Vui lòng chọn tỉnh/thành phố")
    private String provinceCode;

    @NotBlank(groups = AddressUpdate.class, message = "Vui lòng chọn quận/huyện")
    private String districtCode;

    @NotBlank(groups = AddressUpdate.class, message = "Vui lòng chọn phường/xã")
    private String wardCode;

    @NotNull(groups = AddressUpdate.class, message = "Giờ mở cửa không được để trống")
    private LocalTime merchantOpenTime;

    @NotNull(groups = AddressUpdate.class, message = "Giờ đóng cửa không được để trống")
    private LocalTime merchantCloseTime;
}