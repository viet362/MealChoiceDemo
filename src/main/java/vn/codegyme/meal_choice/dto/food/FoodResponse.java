package vn.codegyme.meal_choice.dto.food;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
public class FoodResponse {

    private Long id;

    private UUID merchantAddressId;

    private String merchantAddress;

    private LocalTime merchantOpenTime;

    private LocalTime merchantCloseTime;

    private String foodName;

    private Integer preparationTime;

    private String foodNote;

    // Giá gốc
    private BigDecimal price;

    // Loại khuyến mãi: PERCENT hoặc AMOUNT
    private String discountType;

    // Giá trị khuyến mãi: 20 (%) hoặc 20.000 (VNĐ)
    private BigDecimal discountValue;

    // Giá sau khi áp dụng khuyến mãi
    private BigDecimal discountPrice;

    private BigDecimal serviceFee;

    private Integer views;

    private Integer orderCount;

    private Boolean isRecommended;

    private List<Long> categoryIds;
    private List<String> categoryNames;

    private List<Long> tagIds;
    private List<String> tagNames;

    private List<Long> couponIds;
    private List<String> couponCodes;


    private List<String> imageUrls;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}