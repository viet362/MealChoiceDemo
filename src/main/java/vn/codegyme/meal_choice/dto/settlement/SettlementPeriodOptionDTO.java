package vn.codegyme.meal_choice.dto.settlement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementPeriodOptionDTO {
    private String periodKey;
    private String label;
    private String periodType;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
    private String statusDisplayName;
    private String statusBadgeClass;
}
