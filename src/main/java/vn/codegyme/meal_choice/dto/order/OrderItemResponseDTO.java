package vn.codegyme.meal_choice.dto.order;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponseDTO {

    private Long id;
    private Long foodId;
    private String foodName;
    private String foodImage;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal;
    private String note;
}
