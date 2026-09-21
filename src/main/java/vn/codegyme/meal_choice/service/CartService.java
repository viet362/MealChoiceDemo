package vn.codegyme.meal_choice.service;

import vn.codegyme.meal_choice.dto.cart.AddToCartRequest;
import vn.codegyme.meal_choice.dto.cart.CartResponse;
import vn.codegyme.meal_choice.dto.cart.UpdateCartItemRequest;
import vn.codegyme.meal_choice.entity.Cart;

import java.util.UUID;

/**
 * Nghiệp vụ giỏ hàng của Khách hàng (User).
 *
 * Ràng buộc chung:
 * - Mỗi User có duy nhất một giỏ hàng, tự tạo ở lần dùng đầu tiên.
 * - Giỏ hàng chỉ chứa món của một Merchant tại một thời điểm.
 * - Giá món không lưu trong giỏ, luôn đọc từ bảng foods.
 */
public interface CartService {

    /**
     * Xem thông tin giỏ hàng hiện tại. Tự tạo giỏ rỗng nếu chưa có.
     */
    CartResponse getMyCart(UUID userId);

    /**
     * Thêm món vào giỏ. Nếu món đã có sẵn thì cộng dồn số lượng.
     *
     * @throws vn.codegyme.meal_choice.exception.CartMerchantConflictException
     *         khi giỏ đang có món của quán khác và request không đặt replaceCart = true
     */
    CartResponse addItem(UUID userId, AddToCartRequest request);

    /**
     * Chỉnh sửa một dòng trong giỏ: đổi số lượng (0 = xóa) hoặc sửa ghi chú.
     */
    CartResponse updateItem(UUID userId, Long cartItemId, UpdateCartItemRequest request);

    /**
     * Xóa một món khỏi giỏ hàng.
     */
    CartResponse removeItem(UUID userId, Long cartItemId);

    /**
     * Dọn sạch toàn bộ giỏ hàng
     */
    CartResponse clearCart(UUID userId);

    /**
     * Dọn các món ăn thuộc về một quán cụ thể khỏi giỏ hàng
     */
    CartResponse clearCartForMerchant(UUID userId, UUID merchantId);

    /**
     * Lấy entity giỏ hàng để phục vụ luồng đặt hàng. Dùng nội bộ giữa các service.
     */
    Cart getCartEntity(UUID userId);
}