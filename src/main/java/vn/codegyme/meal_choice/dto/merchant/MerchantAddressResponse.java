package vn.codegyme.meal_choice.dto.merchant;

import lombok.Data;

import java.time.LocalTime;
import java.util.UUID;

@Data
public class MerchantAddressResponse {

    private UUID id;

    private String provinceCode;

    private String districtCode;

    private String wardCode;

    private String merchantAddress;

    private LocalTime merchantOpenTime;

    private LocalTime merchantCloseTime;

    private boolean isDefault;
}