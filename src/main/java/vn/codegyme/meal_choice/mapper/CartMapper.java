package vn.codegyme.meal_choice.mapper;

import org.springframework.stereotype.Component;
import vn.codegyme.meal_choice.dto.cart.CartItemResponse;
import vn.codegyme.meal_choice.dto.cart.CartResponse;
import vn.codegyme.meal_choice.entity.Cart;
import vn.codegyme.meal_choice.entity.CartItem;
import vn.codegyme.meal_choice.entity.Food;
import vn.codegyme.meal_choice.entity.FoodImage;
import vn.codegyme.meal_choice.entity.Merchant;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Chuyển Cart / CartItem sang DTO.
 *
 * Toàn bộ số tiền được tính lại từ Food ở thời điểm gọi, không lấy từ dữ liệu
 * đã lưu trong giỏ hàng, nên giá luôn khớp với giá món hiện tại.
 */
@Component
public class CartMapper {

    /**
     * Giá thực tế của một món: ưu tiên giá khuyến mãi nếu có và lớn hơn 0.
     * Quy tắc này phải luôn khớp với UserOrderServiceImpl#placeOrder.
     */
    public BigDecimal resolveEffectivePrice(Food food) {
        if (food == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = food.getDiscountPrice();

        if (discount != null && discount.compareTo(BigDecimal.ZERO) > 0) {
            return discount;
        }

        return food.getPrice() != null ? food.getPrice() : BigDecimal.ZERO;
    }

    public boolean isAvailable(Food food) {
        return food != null
                && Boolean.TRUE.equals(food.getIsActive())
                && food.getDeletedAt() == null;
    }

    public CartItemResponse toItemResponse(CartItem item) {
        if (item == null) {
            return null;
        }

        Food food = item.getFood();

        int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
        BigDecimal effectivePrice = resolveEffectivePrice(food);
        boolean available = isAvailable(food);

        Merchant itemMerchant = (food != null) ? food.getMerchant() : null;

        return CartItemResponse.builder()
                .id(item.getId())
                .foodId(food != null ? food.getId() : null)
                .foodName(food != null ? food.getFoodName() : "")
                .foodImage(resolvePrimaryImage(food))
                .merchantId(itemMerchant != null ? itemMerchant.getId() : null)
                .merchantName(itemMerchant != null ? itemMerchant.getMerchantRestaurantName() : null)
                .price(food != null && food.getPrice() != null ? food.getPrice() : BigDecimal.ZERO)
                .discountPrice(food != null ? food.getDiscountPrice() : null)
                .effectivePrice(effectivePrice)
                .serviceFee(food != null && food.getServiceFee() != null ? food.getServiceFee() : BigDecimal.ZERO)
                .quantity(quantity)
                .subtotal(effectivePrice.multiply(BigDecimal.valueOf(quantity)))
                .note(item.getNote())
                .preparationTime(food != null ? food.getPreparationTime() : null)
                .available(available)
                .unavailableReason(available ? null : "Món ăn hiện không còn phục vụ")
                .build();
    }

    public CartResponse toCartResponse(Cart cart) {
        if (cart == null) {
            return CartResponse.builder().build();
        }

        List<CartItemResponse> itemDtos = new ArrayList<>();

        int totalItems = 0;
        BigDecimal originalSubtotal = BigDecimal.ZERO;
        BigDecimal subtotalPrice = BigDecimal.ZERO;
        BigDecimal serviceFee = BigDecimal.ZERO;
        boolean hasUnavailable = false;

        if (cart.getItems() != null) {
            for (CartItem item : cart.getItems()) {
                CartItemResponse dto = toItemResponse(item);

                if (dto == null) {
                    continue;
                }

                itemDtos.add(dto);

                totalItems += dto.getQuantity();

                originalSubtotal = originalSubtotal.add(
                        dto.getPrice().multiply(BigDecimal.valueOf(dto.getQuantity()))
                );

                subtotalPrice = subtotalPrice.add(dto.getSubtotal());

                if (dto.getServiceFee().compareTo(serviceFee) > 0) {
                    serviceFee = dto.getServiceFee();
                }

                if (!Boolean.TRUE.equals(dto.getAvailable())) {
                    hasUnavailable = true;
                }
            }
        }

        // Giỏ rỗng thì không tính phí dịch vụ
        if (itemDtos.isEmpty()) {
            serviceFee = BigDecimal.ZERO;
        }

        // [ĐA QUÁN] merchantId/Name ở cấp Cart không còn ý nghĩa, thông tin này đã được đưa vào từng CartItemResponse
        return CartResponse.builder()
                .id(cart.getId())
                .merchantId(null)
                .merchantName(null)
                .merchantBankName(null)
                .merchantBankAccountNumber(null)
                .items(itemDtos)
                .totalItems(totalItems)
                .originalSubtotal(originalSubtotal)
                .subtotalPrice(subtotalPrice)
                .savings(originalSubtotal.subtract(subtotalPrice).max(BigDecimal.ZERO))
                .serviceFee(serviceFee)
                .estimatedTotal(subtotalPrice.add(serviceFee))
                .hasUnavailableItems(hasUnavailable)
                .empty(itemDtos.isEmpty())
                .build();
    }

    private String resolvePrimaryImage(Food food) {
        if (food == null || food.getImages() == null || food.getImages().isEmpty()) {
            return null;
        }

        try {
            return food.getImages().stream()
                    .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                    .map(FoodImage::getImageUrl)
                    .findFirst()
                    .orElse(food.getImages().get(0).getImageUrl());
        } catch (Exception e) {
            return null;
        }
    }
}