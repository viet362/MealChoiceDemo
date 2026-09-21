package vn.codegyme.meal_choice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressResponseDTO {

    private Long id;
    private String contactName;
    private String contactPhone;
    private String city;
    private String district;
    private String ward;
    private String street;
    private String note;
    private Boolean isDefault;
}
