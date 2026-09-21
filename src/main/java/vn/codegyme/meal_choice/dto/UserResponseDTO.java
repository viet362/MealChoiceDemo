package vn.codegyme.meal_choice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.codegyme.meal_choice.entity.Gender;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {

    private UUID id;
    private String displayName;
    private String email;
    private String phoneNumber;
    private String avatarUrl;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dob;

    private Gender gender;
    private List<AddressResponseDTO> addresses;
    private Boolean isActive;
}
