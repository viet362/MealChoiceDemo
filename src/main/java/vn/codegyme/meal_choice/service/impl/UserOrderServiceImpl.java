package vn.codegyme.meal_choice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegyme.meal_choice.dto.Delivery.GeoPoint;
import vn.codegyme.meal_choice.dto.order.CheckoutRequestDTO;
import vn.codegyme.meal_choice.dto.order.OrderResponseDTO;
import vn.codegyme.meal_choice.entity.*;
import vn.codegyme.meal_choice.mapper.CartMapper;
import vn.codegyme.meal_choice.mapper.OrderMapper;
import vn.codegyme.meal_choice.repository.*;
import vn.codegyme.meal_choice.service.CartService;
import vn.codegyme.meal_choice.service.DistanceService;
import vn.codegyme.meal_choice.service.GeocodingService;
import vn.codegyme.meal_choice.service.ShippingFeeService;
import vn.codegyme.meal_choice.service.UserOrderService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserOrderServiceImpl implements UserOrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final FoodRepository foodRepository;
    private final OrderMapper orderMapper;
    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final MerchantAddressRepository merchantAddressRepository;
    private final AddressRepository addressRepository;
    private final ShippingFeeService shippingFeeService;
    private final DistanceService distanceService;
    private final GeocodingService geocodingService;

    private final CartService cartService;
    private final CartMapper cartMapper;
    private final CouponRepository couponRepository;

    /** Phí giao hàng cố định dự phòng: 15.000 đ */
    private static final BigDecimal DEFAULT_SHIPPING_FEE = BigDecimal.valueOf(15000);

    /** Ngưỡng khoảng cách giao hàng tối đa dùng để tính cước (km) */
    private static final double MAX_DELIVERY_DISTANCE_KM = 30.0;

    // =====================================================================
    // ĐẶT HÀNG TỪ GIỎ HÀNG
    // =====================================================================

    @Override
    @Transactional
    public OrderResponseDTO placeOrder(UUID userId, CheckoutRequestDTO request) {

        log.info("Bắt đầu đặt hàng từ giỏ hàng của user {} cho quán {}", userId, request.getMerchantId());

        if (request.getMerchantId() == null) {
            throw new IllegalArgumentException("Không xác định được quán cần đặt hàng");
        }

        // ---------- BƯỚC 1: Người dùng ----------
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin người dùng"));

        // ---------- BƯỚC 2: Đọc giỏ hàng và lọc món của quán ----------
        Cart cart = cartService.getCartEntity(userId);

        if (cart == null || cart.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng của bạn đang trống");
        }

        List<CartItem> cartItems = cart.getItems().stream()
                .filter(item -> item.getFood() != null
                        && item.getFood().getMerchant() != null
                        && item.getFood().getMerchant().getId().equals(request.getMerchantId()))
                .toList();

        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng không có món nào thuộc quán này");
        }

        Merchant merchant = cartItems.get(0).getFood().getMerchant();

        if (merchant.getMerchantStatus() != MerchantStatus.APPROVED) {
            throw new IllegalArgumentException(
                    "Cửa hàng \"" + merchant.getMerchantRestaurantName() + "\" hiện không nhận đơn hàng");
        }

        // ---------- BƯỚC 3: Duyệt từng món, tính tiền theo giá HIỆN TẠI ----------
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal maxServiceFee = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cartItems) {

            Food food = cartItem.getFood();

            if (food == null) {
                throw new IllegalArgumentException(
                        "Một món trong giỏ hàng không còn tồn tại. Vui lòng kiểm tra lại giỏ hàng");
            }

            if (!cartMapper.isAvailable(food)) {
                throw new IllegalArgumentException(
                        "Món \"" + food.getFoodName()
                                + "\" hiện không còn phục vụ. Vui lòng bỏ món này khỏi giỏ hàng");
            }

            if (food.getMerchant() == null
                    || !food.getMerchant().getId().equals(merchant.getId())) {
                throw new IllegalArgumentException(
                        "Món \"" + food.getFoodName() + "\" không thuộc cửa hàng này");
            }

            int quantity = cartItem.getQuantity() != null ? cartItem.getQuantity() : 0;

            if (quantity < 1) {
                continue;
            }

            // Giá lấy trực tiếp từ bảng foods tại thời điểm đặt
            BigDecimal unitPrice = cartMapper.resolveEffectivePrice(food);
            BigDecimal itemSubtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

            subtotal = subtotal.add(itemSubtotal);

            if (food.getServiceFee() != null
                    && food.getServiceFee().compareTo(maxServiceFee) > 0) {
                maxServiceFee = food.getServiceFee();
            }

            OrderItem orderItem = OrderItem.builder()
                    .food(food)
                    .foodName(food.getFoodName())
                    .foodImage(findPrimaryFoodImage(food))
                    .price(unitPrice)
                    .quantity(quantity)
                    .subtotal(itemSubtotal)
                    .note(cartItem.getNote())
                    .build();

            orderItems.add(orderItem);

            food.setOrderCount((food.getOrderCount() != null ? food.getOrderCount() : 0) + quantity);
            foodRepository.save(food);
        }

        if (orderItems.isEmpty()) {
            throw new IllegalArgumentException("Đơn hàng phải có ít nhất 1 món ăn");
        }

        // ---------- BƯỚC 4: Phí vận chuyển ----------
        DeliveryPartner deliveryPartner = null;
        BigDecimal shippingFee = DEFAULT_SHIPPING_FEE;
        double distanceKm = 3.0;

        if (request.getDeliveryPartnerId() != null) {
            deliveryPartner = deliveryPartnerRepository
                    .findById(request.getDeliveryPartnerId())
                    .orElse(null);
        }

        if (deliveryPartner == null) {
            List<DeliveryPartner> activePartners =
                    deliveryPartnerRepository.findByStatus(DeliveryPartnerStatus.ACTIVE);

            if (!activePartners.isEmpty()) {
                deliveryPartner = activePartners.get(0);
            }
        }

        if (deliveryPartner != null) {
            try {
                List<MerchantAddress> merchantAddrs =
                        merchantAddressRepository.findByMerchantId(merchant.getId());

                if (!merchantAddrs.isEmpty()) {
                    MerchantAddress mAddr = merchantAddrs.get(0);

                    GeoPoint mPoint = (mAddr.getLatitude() != null && mAddr.getLongitude() != null)
                            ? new GeoPoint(mAddr.getLatitude(), mAddr.getLongitude())
                            : geocodingService.geocode(mAddr.getMerchantAddress() + ", Việt Nam");

                    // Ưu tiên dùng tọa độ đã lưu trong DB (cùng nguồn với DeliveryQuoteService)
                    // để tránh geocode lại từ text gây ra khoảng cách khác nhau
                    GeoPoint uPoint = null;
                    if (request.getAddressId() != null) {
                        Address savedAddr =
                                addressRepository.findById(request.getAddressId()).orElse(null);
                        if (savedAddr != null && savedAddr.getLatitude() != null && savedAddr.getLongitude() != null) {
                            uPoint = new GeoPoint(savedAddr.getLatitude(), savedAddr.getLongitude());
                        }
                    }

                    // Fallback: geocode từ text nếu không có addressId hoặc chưa có tọa độ
                    if (uPoint == null && request.getDeliveryAddress() != null) {
                        uPoint = geocodingService.geocode(request.getDeliveryAddress() + ", Việt Nam");
                    }

                    if (mPoint != null && uPoint != null) {
                        distanceKm = clampDistance(
                                distanceService.calculateDistanceKm(mPoint, uPoint));
                    }
                }

                shippingFee = shippingFeeService.calculateShippingFee(deliveryPartner, distanceKm);

            } catch (Exception e) {
                log.warn("Không tính được phí ship chính xác, dùng cước cơ bản: {}", e.getMessage());
                shippingFee = (deliveryPartner.getBaseFee() != null)
                        ? deliveryPartner.getBaseFee()
                        : DEFAULT_SHIPPING_FEE;
            }
        }

        // ---------- BƯỚC 5: Phí dịch vụ, voucher, tổng tiền ----------
        BigDecimal serviceFee = maxServiceFee;
        BigDecimal discountAmount = calculateVoucherDiscount(
                request.getVoucherCode(), merchant.getId(), orderItems, subtotal, shippingFee);

        BigDecimal totalAmount = subtotal
                .add(shippingFee)
                .add(serviceFee)
                .subtract(discountAmount);

        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
        }

        // ---------- BƯỚC 6: Thời gian dự kiến ----------
        LocalDateTime now = LocalDateTime.now();

        int prepMinutes = orderItems.stream()
                .mapToInt(item -> (item.getFood() != null
                        && item.getFood().getPreparationTime() != null
                        && item.getFood().getPreparationTime() > 0)
                        ? item.getFood().getPreparationTime()
                        : 10)
                .max()
                .orElse(10);

        prepMinutes = Math.max(5, prepMinutes);

        int deliveryTransitMinutes = (int) Math.max(0.5, Math.round(distanceKm * 0.5));
        LocalDateTime estimatedDelivery = now.plusMinutes(prepMinutes + deliveryTransitMinutes);

        // ---------- BƯỚC 7: Lưu đơn hàng ----------
        Order order = Order.builder()
                .orderCode(generateOrderCode())
                .user(user)
                .merchant(merchant)
                .deliveryPartner(deliveryPartner)
                .contactName(request.getContactName())
                .contactPhone(request.getContactPhone())
                .deliveryAddress(request.getDeliveryAddress())
                .note(request.getNote())
                .status(OrderStatus.PENDING)
                .paymentMethod(request.getPaymentMethod() != null
                        ? request.getPaymentMethod()
                        : PaymentMethod.COD)
                .subtotalPrice(subtotal)
                .shippingFee(shippingFee)
                .serviceFee(serviceFee)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .estimatedDeliveryTime(estimatedDelivery)
                .createdAt(now)
                .updatedAt(now)
                .build();

        for (OrderItem item : orderItems) {
            order.addOrderItem(item);
        }

        Order savedOrder = orderRepository.save(order);

        // ---------- BƯỚC 8: Chỉ dọn sạch các món của quán này khỏi giỏ hàng ----------
        cartService.clearCartForMerchant(userId, request.getMerchantId());

        // ---------- BƯỚC 9: Lưu Log thay đổi trạng thái ----------
        log.info("Đặt hàng thành công, mã đơn {}", savedOrder.getOrderCode());

        return decorate(orderMapper.toOrderResponseDTO(savedOrder), savedOrder);
    }

    // =====================================================================
    // XEM DANH SÁCH ĐƠN HÀNG
    // =====================================================================

    @Override
    @Transactional
    public List<OrderResponseDTO> getUserOrders(UUID userId) {

        List<Order> orders = orderRepository.findByUser_IdOrderByIdDesc(userId);

        orders.forEach(this::autoSyncOrderStatus);

        List<OrderResponseDTO> dtos = orderMapper.toOrderResponseDTOList(orders);

        for (int i = 0; i < dtos.size(); i++) {
            decorate(dtos.get(i), orders.get(i));
        }

        return dtos;
    }

    @Override
    @Transactional
    public Page<OrderResponseDTO> getUserOrders(UUID userId, Pageable pageable) {

        Page<Order> orderPage = orderRepository.findByUser_IdOrderByIdDesc(userId, pageable);

        orderPage.getContent().forEach(this::autoSyncOrderStatus);

        return orderPage.map(order -> decorate(orderMapper.toOrderResponseDTO(order), order));
    }

    @Override
    @Transactional
    public Page<OrderResponseDTO> getUserOrders(UUID userId, OrderStatus status, Pageable pageable) {

        Page<Order> orderPage = (status != null)
                ? orderRepository.findByUser_IdAndStatusOrderByIdDesc(userId, status, pageable)
                : orderRepository.findByUser_IdOrderByIdDesc(userId, pageable);

        orderPage.getContent().forEach(this::autoSyncOrderStatus);

        return orderPage.map(order -> decorate(orderMapper.toOrderResponseDTO(order), order));
    }

    // =====================================================================
    // XEM CHI TIẾT MỘT ĐƠN HÀNG
    // =====================================================================

    @Override
    @Transactional
    public OrderResponseDTO getUserOrderDetail(UUID userId, Long orderId) {

        Order order = findOwnedOrderOrThrow(userId, orderId);

        autoSyncOrderStatus(order);

        return decorate(orderMapper.toOrderResponseDTO(order), order);
    }

    @Override
    @Transactional
    public OrderResponseDTO getOrderDetailByCode(String orderCode) {

        Order order = orderRepository.findByOrderCodeWithItems(orderCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy đơn hàng với mã: " + orderCode));

        autoSyncOrderStatus(order);

        return decorate(orderMapper.toOrderResponseDTO(order), order);
    }

    @Override
    @Transactional
    public OrderResponseDTO getOrderDetailByCode(String orderCode, UUID userId) {

        Order order = orderRepository.findByOrderCodeAndUserIdWithItems(orderCode, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy đơn hàng với mã: " + orderCode));

        autoSyncOrderStatus(order);

        return decorate(orderMapper.toOrderResponseDTO(order), order);
    }

    // =====================================================================
    // KHÁCH HÀNG HỦY ĐƠN
    // =====================================================================

    @Override
    @Transactional
    public OrderResponseDTO cancelOrderByUser(UUID userId, Long orderId, String cancelReason) {

        Order order = findOwnedOrderOrThrow(userId, orderId);

        // Lý do hủy là bắt buộc
        if (cancelReason == null || cancelReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do hủy đơn hàng");
        }

        String reason = cancelReason.trim();

        if (reason.length() > 400) {
            reason = reason.substring(0, 400);
        }

        // Đồng bộ trạng thái trước khi kiểm tra, tránh hủy nhầm đơn quán đã nhận
        autoSyncOrderStatus(order);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Đơn hàng này đã được hủy trước đó");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "Quán đã tiếp nhận đơn hàng nên không thể hủy. "
                            + "Vui lòng liên hệ trực tiếp với cửa hàng.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason("Khách hàng hủy: " + reason);
        order.setUpdatedAt(LocalDateTime.now());

        // Trả lại lượt đặt đã cộng cho món khi tạo đơn, để thống kê không bị lệch
        rollbackFoodOrderCount(order);

        Order savedOrder = orderRepository.save(order);

        log.info("User {} đã hủy đơn hàng ID {}, lý do: {}", userId, orderId, reason);

        return decorate(orderMapper.toOrderResponseDTO(savedOrder), savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCancellableByUser(UUID userId, Long orderId) {

        return orderRepository.findByIdAndUser_Id(orderId, userId)
                .map(order -> order.getStatus() == OrderStatus.PENDING)
                .orElse(false);
    }

    // =====================================================================
    // HELPER
    // =====================================================================

    private Order findOwnedOrderOrThrow(UUID userId, Long orderId) {

        if (orderId == null) {
            throw new IllegalArgumentException("Không xác định được đơn hàng");
        }

        return orderRepository.findByIdAndUserIdWithItems(orderId, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy đơn hàng hoặc đơn hàng không thuộc về bạn"));
    }

    /**
     * Gắn thêm cờ cho phép hủy vào DTO.
     */
    private OrderResponseDTO decorate(OrderResponseDTO dto, Order order) {
        if (dto == null) {
            return null;
        }

        dto.setCanCancelByUser(order != null && order.getStatus() == OrderStatus.PENDING);

        return dto;
    }

    /**
     * Tự chuyển PREPARING sang DELIVERING khi đã quá thời gian chuẩn bị.
     */
    private void autoSyncOrderStatus(Order order) {
        if (order == null) {
            return;
        }

        if (order.getStatus() == OrderStatus.PREPARING
                && order.getPreparingUntil() != null
                && LocalDateTime.now().isAfter(order.getPreparingUntil())) {

            order.setStatus(OrderStatus.DELIVERING);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            log.info("Tự động chuyển đơn hàng ID {} sang DELIVERING", order.getId());
        }
    }

    /**
     * Trừ lại orderCount đã cộng khi tạo đơn.
     */
    private void rollbackFoodOrderCount(Order order) {
        if (order.getOrderItems() == null) {
            return;
        }

        for (OrderItem item : order.getOrderItems()) {

            Food food = item.getFood();

            if (food == null || item.getQuantity() == null) {
                continue;
            }

            int current = food.getOrderCount() != null ? food.getOrderCount() : 0;
            food.setOrderCount(Math.max(0, current - item.getQuantity()));

            foodRepository.save(food);
        }
    }

    /**
     * Tính số tiền giảm theo mã voucher của Merchant áp dụng cho các món ăn trong đơn hàng.
     */
    private BigDecimal calculateVoucherDiscount(String voucherCode,
                                                UUID merchantId,
                                                List<OrderItem> orderItems,
                                                BigDecimal subtotal,
                                                BigDecimal shippingFee) {

        if (voucherCode == null
                || voucherCode.trim().isEmpty()
                || subtotal == null
                || subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        String code = voucherCode.trim().toUpperCase();

        // 1. Tìm coupon của quán trong cơ sở dữ liệu
        Optional<Coupon> couponOpt = couponRepository.findByMerchant_IdAndCouponCode(merchantId, code);
        if (couponOpt.isEmpty()) {
            if ("FREESHIP".equalsIgnoreCase(code)) {
                return shippingFee != null ? shippingFee : BigDecimal.ZERO;
            }
            return BigDecimal.ZERO;
        }

        Coupon coupon = couponOpt.get();
        if (!Boolean.TRUE.equals(coupon.getIsActive())) {
            return BigDecimal.ZERO;
        }

        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStartAt() != null && now.isBefore(coupon.getStartAt())) {
            return BigDecimal.ZERO;
        }
        if (coupon.getEndAt() != null && now.isAfter(coupon.getEndAt())) {
            return BigDecimal.ZERO;
        }
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() != null
                && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            return BigDecimal.ZERO;
        }

        // 2. Tìm xem coupon này áp dụng cho những món ăn nào trong đơn hàng
        List<Food> couponFoods = coupon.getFoods();
        BigDecimal eligibleSubtotal = BigDecimal.ZERO;

        if (couponFoods == null || couponFoods.isEmpty()) {
            // Không giới hạn món cụ thể -> Áp dụng toàn quán
            eligibleSubtotal = subtotal;
        } else {
            Set<Long> allowedFoodIds = couponFoods.stream()
                    .map(Food::getId)
                    .collect(Collectors.toSet());

            for (OrderItem item : orderItems) {
                if (item.getFood() != null && allowedFoodIds.contains(item.getFood().getId())) {
                    eligibleSubtotal = eligibleSubtotal.add(item.getSubtotal());
                }
            }
        }

        // Nếu không có món nào trong đơn hàng đủ điều kiện
        if (eligibleSubtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (coupon.getDiscountType() == DiscountType.PERCENT) {
            discount = eligibleSubtotal
                    .multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        } else if (coupon.getDiscountType() == DiscountType.FIXED) {
            discount = coupon.getDiscountValue().min(eligibleSubtotal);
        }

        BigDecimal ceiling = subtotal.add(shippingFee != null ? shippingFee : BigDecimal.ZERO);
        return discount.min(ceiling);
    }

    /**
     * Giới hạn khoảng cách trong ngưỡng giao đồ ăn hợp lý.
     *
     * Thay cho đoạn hard-code riêng cho Hà Nội trước đây: khi geocoding trả về
     * tọa độ sai tỉnh, khoảng cách có thể lên hàng trăm km và phí ship bị thổi phồng.
     */
    private double clampDistance(double distanceKm) {
        if (distanceKm <= 0 || Double.isNaN(distanceKm)) {
            return 3.0;
        }

        return Math.min(distanceKm, MAX_DELIVERY_DISTANCE_KM);
    }

    private String findPrimaryFoodImage(Food food) {
        if (food.getImages() == null || food.getImages().isEmpty()) {
            return null;
        }

        return food.getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .map(FoodImage::getImageUrl)
                .findFirst()
                .orElse(food.getImages().get(0).getImageUrl());
    }

    private String generateOrderCode() {
        return "MC-"
                + System.currentTimeMillis() % 10000000
                + "-"
                + String.format("%03d", new Random().nextInt(1000));
    }
}