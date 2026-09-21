package vn.codegyme.meal_choice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegyme.meal_choice.dto.cart.AddToCartRequest;
import vn.codegyme.meal_choice.dto.cart.CartResponse;
import vn.codegyme.meal_choice.dto.cart.UpdateCartItemRequest;
import vn.codegyme.meal_choice.entity.Cart;
import vn.codegyme.meal_choice.entity.CartItem;
import vn.codegyme.meal_choice.entity.Food;
import vn.codegyme.meal_choice.entity.Merchant;
import vn.codegyme.meal_choice.entity.MerchantStatus;
import vn.codegyme.meal_choice.entity.User;
import vn.codegyme.meal_choice.exception.CartMerchantConflictException;
import vn.codegyme.meal_choice.mapper.CartMapper;
import vn.codegyme.meal_choice.repository.CartItemRepository;
import vn.codegyme.meal_choice.repository.CartRepository;
import vn.codegyme.meal_choice.repository.FoodRepository;
import vn.codegyme.meal_choice.repository.UserRepository;
import vn.codegyme.meal_choice.service.CartService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private static final int MAX_QUANTITY_PER_ITEM = 99;
    private static final int MAX_DISTINCT_ITEMS = 50;

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final FoodRepository foodRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;

    // ==================== XEM GIỎ HÀNG ====================

    @Override
    @Transactional
    public CartResponse getMyCart(UUID userId) {
        Cart cart = getCartEntity(userId);

        // Dọn các món đã bị merchant xóa hẳn khỏi hệ thống trước khi trả về
        pruneDeletedFoods(cart);

        return toResponse(cart);
    }

    // ==================== THÊM MÓN ====================

    @Override
    @Transactional
    public CartResponse addItem(UUID userId, AddToCartRequest request) {

        if (request == null || request.getFoodId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn món ăn cần thêm vào giỏ hàng");
        }

        int quantityToAdd = request.getQuantity() != null ? request.getQuantity() : 1;

        if (quantityToAdd < 1) {
            throw new IllegalArgumentException("Số lượng phải lớn hơn hoặc bằng 1");
        }

        Food food = foodRepository.findById(request.getFoodId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy món ăn"));

        if (!cartMapper.isAvailable(food)) {
            throw new IllegalArgumentException(
                    "Món \"" + food.getFoodName() + "\" hiện không còn phục vụ");
        }

        Merchant foodMerchant = food.getMerchant();

        if (foodMerchant == null) {
            throw new IllegalArgumentException("Món ăn chưa được gắn với cửa hàng nào");
        }

        if (foodMerchant.getMerchantStatus() != MerchantStatus.APPROVED) {
            throw new IllegalArgumentException(
                    "Cửa hàng \"" + foodMerchant.getMerchantRestaurantName()
                            + "\" hiện không nhận đơn hàng");
        }

        Cart cart = getCartEntity(userId);
        pruneDeletedFoods(cart);

        // ===== [ĐA QUÁN] Ràng buộc một quán đã được TẮT để cho phép giỏ hàng từ nhiều quán =====
        // Merchant cartMerchant = cart.getMerchant();
        //
        // boolean differentMerchant = !cart.isEmpty()
        //         && cartMerchant != null
        //         && !cartMerchant.getId().equals(foodMerchant.getId());
        //
        // if (differentMerchant) {
        //     if (!Boolean.TRUE.equals(request.getReplaceCart())) {
        //         throw new CartMerchantConflictException(
        //                 cartMerchant.getId(),
        //                 cartMerchant.getMerchantRestaurantName(),
        //                 foodMerchant.getId(),
        //                 foodMerchant.getMerchantRestaurantName()
        //         );
        //     }
        //
        //     cart.clear();
        //     cartRepository.saveAndFlush(cart);
        // }
        //
        // cart.setMerchant(foodMerchant); // Không còn set vì giỏ có thể có nhiều quán

        // ===== Cộng dồn nếu món đã có trong giỏ =====
        CartItem existing = findItemByFoodId(cart, food.getId());

        if (existing != null) {
            int newQuantity = existing.getQuantity() + quantityToAdd;

            if (newQuantity > MAX_QUANTITY_PER_ITEM) {
                newQuantity = MAX_QUANTITY_PER_ITEM;
            }

            existing.setQuantity(newQuantity);

            if (request.getNote() != null && !request.getNote().isBlank()) {
                existing.setNote(trimNote(request.getNote()));
            }

        } else {
            if (cart.getItems().size() >= MAX_DISTINCT_ITEMS) {
                throw new IllegalArgumentException(
                        "Giỏ hàng chỉ chứa tối đa " + MAX_DISTINCT_ITEMS + " món khác nhau");
            }

            CartItem item = CartItem.builder()
                    .food(food)
                    .quantity(Math.min(quantityToAdd, MAX_QUANTITY_PER_ITEM))
                    .note(trimNote(request.getNote()))
                    .build();

            cart.addItem(item);
        }

        Cart saved = cartRepository.save(cart);
        log.info("User {} đã thêm món {} vào giỏ hàng", userId, food.getId());

        return toResponse(saved);
    }

    // ==================== SỬA MÓN TRONG GIỎ ====================

    @Override
    @Transactional
    public CartResponse updateItem(UUID userId, Long cartItemId, UpdateCartItemRequest request) {

        if (cartItemId == null) {
            throw new IllegalArgumentException("Không xác định được món cần cập nhật");
        }

        Cart cart = getCartEntity(userId);

        CartItem item = findItemById(cart, cartItemId);

        if (item == null) {
            throw new IllegalArgumentException("Món này không có trong giỏ hàng của bạn");
        }

        if (request == null) {
            return toResponse(cart);
        }

        // ===== Tính số lượng mới =====
        Integer newQuantity = null;

        if (request.getQuantity() != null) {
            newQuantity = request.getQuantity();
        } else if (request.getQuantityDelta() != null) {
            newQuantity = item.getQuantity() + request.getQuantityDelta();
        }

        if (newQuantity != null) {

            if (newQuantity <= 0) {
                // Số lượng về 0 nghĩa là bỏ món khỏi giỏ
                cart.removeItem(item);
                cartItemRepository.delete(item);

                if (cart.isEmpty()) {
                    cart.setMerchant(null);
                }

                return toResponse(cartRepository.save(cart));
            }

            if (newQuantity > MAX_QUANTITY_PER_ITEM) {
                newQuantity = MAX_QUANTITY_PER_ITEM;
            }

            item.setQuantity(newQuantity);
        }

        // ===== Ghi chú =====
        if (request.getNote() != null) {
            item.setNote(trimNote(request.getNote()));
        }

        Cart saved = cartRepository.save(cart);

        return toResponse(saved);
    }

    // ==================== XÓA MỘT MÓN ====================

    @Override
    @Transactional
    public CartResponse removeItem(UUID userId, Long cartItemId) {

        Cart cart = getCartEntity(userId);

        CartItem item = findItemById(cart, cartItemId);

        if (item == null) {
            throw new IllegalArgumentException("Món này không có trong giỏ hàng của bạn");
        }

        cart.removeItem(item);
        cartItemRepository.delete(item);

        if (cart.isEmpty()) {
            cart.setMerchant(null);
        }

        Cart saved = cartRepository.save(cart);
        log.info("User {} đã xóa món khỏi giỏ hàng (cartItemId = {})", userId, cartItemId);

        return toResponse(saved);
    }

    // ==================== XÓA TOÀN BỘ GIỎ ====================

    @Override
    @Transactional
    public CartResponse clearCart(UUID userId) {

        Cart cart = getCartEntity(userId);

        cart.clear();

        Cart saved = cartRepository.save(cart);
        log.info("User {} đã xóa toàn bộ giỏ hàng", userId);

        return toResponse(saved);
    }

    // ==================== XÓA GIỎ THEO QUÁN ====================

    @Override
    @Transactional
    public CartResponse clearCartForMerchant(UUID userId, UUID merchantId) {
        if (merchantId == null) {
            throw new IllegalArgumentException("Không xác định được cửa hàng cần xóa");
        }

        Cart cart = getCartEntity(userId);

        List<CartItem> itemsToRemove = cart.getItems().stream()
                .filter(item -> item.getFood() != null
                        && item.getFood().getMerchant() != null
                        && item.getFood().getMerchant().getId().equals(merchantId))
                .toList();

        for (CartItem item : itemsToRemove) {
            cart.removeItem(item);
            cartItemRepository.delete(item);
        }

        Cart saved = cartRepository.save(cart);
        log.info("User {} đã xóa các món của quán {} khỏi giỏ hàng", userId, merchantId);

        return toResponse(saved);
    }

    // ==================== DÙNG NỘI BỘ ====================

    @Override
    @Transactional
    public Cart getCartEntity(UUID userId) {

        if (userId == null) {
            throw new IllegalArgumentException("Không xác định được người dùng");
        }

        return cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> createCartForUser(userId));
    }

    // ==================== HELPER ====================

    /**
     * Nạp ảnh món rồi mới chuyển sang DTO.
     */
    private CartResponse toResponse(Cart cart) {
        warmFoodImages(cart);
        return cartMapper.toCartResponse(cart);
    }

    /**
     * Nạp trước ảnh cho toàn bộ món trong giỏ bằng MỘT truy vấn.
     *
     * Cart.items và Food.images đều là List nên không thể fetch join chung một câu
     * (MultipleBagFetchException). Tách ra hai truy vấn vừa tránh được lỗi đó,
     * vừa không rơi vào N+1 khi CartMapper đọc food.getImages().
     */
    private void warmFoodImages(Cart cart) {
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            return;
        }

        List<Long> foodIds = cart.getItems().stream()
                .map(CartItem::getFood)
                .filter(Objects::nonNull)
                .map(Food::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (foodIds.isEmpty()) {
            return;
        }

        // Các Food trả về chính là instance đang nằm trong persistence context,
        // nên sau lời gọi này collection images đã được khởi tạo sẵn.
        cartRepository.findFoodsWithImagesByIds(foodIds);
    }

    private Cart createCartForUser(UUID userId) {

        // Có thể giỏ đã tồn tại nhưng chưa có item nên câu JOIN FETCH không trả về
        return cartRepository.findByUser_Id(userId).orElseGet(() -> {

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin người dùng"));

            Cart cart = Cart.builder()
                    .user(user)
                    .items(new ArrayList<>())
                    .build();

            return cartRepository.save(cart);
        });
    }

    private CartItem findItemById(Cart cart, Long cartItemId) {
        if (cart.getItems() == null || cartItemId == null) {
            return null;
        }

        return cart.getItems().stream()
                .filter(i -> i.getId() != null && i.getId().equals(cartItemId))
                .findFirst()
                .orElse(null);
    }

    private CartItem findItemByFoodId(Cart cart, Long foodId) {
        if (cart.getItems() == null || foodId == null) {
            return null;
        }

        return cart.getItems().stream()
                .filter(i -> i.getFood() != null && foodId.equals(i.getFood().getId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Bỏ khỏi giỏ những món đã bị merchant xóa mềm.
     * Món chỉ bị tắt hiển thị (isActive = false) vẫn giữ lại để người dùng
     * nhìn thấy cảnh báo và tự quyết định.
     */
    private void pruneDeletedFoods(Cart cart) {
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            return;
        }

        List<CartItem> removable = cart.getItems().stream()
                .filter(i -> i.getFood() == null || i.getFood().getDeletedAt() != null)
                .toList();

        if (removable.isEmpty()) {
            return;
        }

        for (CartItem item : removable) {
            cart.removeItem(item);
            cartItemRepository.delete(item);
        }

        if (cart.isEmpty()) {
            cart.setMerchant(null);
        }

        cartRepository.save(cart);
    }

    private String trimNote(String note) {
        if (note == null) {
            return null;
        }

        String trimmed = note.trim();

        if (trimmed.isEmpty()) {
            return null;
        }

        return trimmed.length() > 255 ? trimmed.substring(0, 255) : trimmed;
    }
}