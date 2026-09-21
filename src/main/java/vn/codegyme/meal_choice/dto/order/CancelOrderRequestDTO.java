package vn.codegyme.meal_choice.dto.order;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelOrderRequestDTO {

    private String cancelReason;
}
