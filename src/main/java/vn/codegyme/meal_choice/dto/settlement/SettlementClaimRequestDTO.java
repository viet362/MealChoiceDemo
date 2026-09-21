package vn.codegyme.meal_choice.dto.settlement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.codegyme.meal_choice.entity.SettlementClaimReason;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementClaimRequestDTO {

    @NotNull(message = "Vui lòng chọn lý do khiếu nại")
    private SettlementClaimReason reason;

    @NotBlank(message = "Vui lòng nhập mô tả chi tiết lý do khiếu nại")
    private String description;
}
