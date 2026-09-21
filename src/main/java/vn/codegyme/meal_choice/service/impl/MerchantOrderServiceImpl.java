package vn.codegyme.meal_choice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegyme.meal_choice.dto.Delivery.GeoPoint;
import vn.codegyme.meal_choice.dto.order.OrderResponseDTO;
import vn.codegyme.meal_choice.entity.MerchantAddress;
import vn.codegyme.meal_choice.entity.Order;
import vn.codegyme.meal_choice.entity.OrderStatus;
import vn.codegyme.meal_choice.mapper.OrderMapper;
import vn.codegyme.meal_choice.repository.MerchantAddressRepository;
import vn.codegyme.meal_choice.repository.OrderRepository;
import vn.codegyme.meal_choice.service.DistanceService;
import vn.codegyme.meal_choice.service.GeocodingService;
import vn.codegyme.meal_choice.service.MerchantOrderService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantOrderServiceImpl implements MerchantOrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final MerchantAddressRepository merchantAddressRepository;
    private final GeocodingService geocodingService;
    private final DistanceService distanceService;

    /**
     * TÍNH NĂNG 1: Lấy danh sách đơn hàng của Merchant (có thể lọc theo trạng thái)
     */
    @Override
    @Transactional
    public List<OrderResponseDTO> getMerchantOrders(UUID merchantId, OrderStatus status) {
        List<Order> orders;
        if (status != null) {
            orders = orderRepository.findByMerchant_IdAndStatusOrderByIdDesc(merchantId, status);
        } else {
            orders = orderRepository.findByMerchant_IdOrderByIdDesc(merchantId);
        }

        // Tự động đồng bộ trạng thái nếu đã hết thời gian chuẩn bị
        for (Order o : orders) {
            autoSyncOrderStatus(o);
        }

        return orderMapper.toOrderResponseDTOList(orders);
    }

    /**
     * TÍNH NĂNG 1: Lấy danh sách đơn hàng của Merchant có phân trang (mặc định mới nhất đến cũ nhất theo ID)
     */
    @Override
    @Transactional
    public Page<OrderResponseDTO> getMerchantOrders(UUID merchantId, OrderStatus status, Pageable pageable) {
        Page<Order> orderPage;
        if (status != null) {
            orderPage = orderRepository.findByMerchant_IdAndStatusOrderByIdDesc(merchantId, status, pageable);
        } else {
            orderPage = orderRepository.findByMerchant_IdOrderByIdDesc(merchantId, pageable);
        }

        // Tự động đồng bộ trạng thái nếu đã hết thời gian chuẩn bị
        for (Order o : orderPage.getContent()) {
            autoSyncOrderStatus(o);
        }

        return orderMapper.toOrderResponseDTOPage(orderPage);
    }

    // Tìm kiếm đơn hàng theo mã, tên khách hàng hoặc số điện thoại
    @Override
    @Transactional
    public Page<OrderResponseDTO> getMerchantOrders(
            UUID merchantId,
            OrderStatus status,
            String keyword,
            Pageable pageable) {

        String searchKeyword = keyword == null ? "" : keyword.trim();
        Page<Order> orderPage;

        if (searchKeyword.isBlank()) {
            if (status != null) {
                orderPage = orderRepository.findByMerchant_IdAndStatusOrderByIdDesc(
                        merchantId,
                        status,
                        pageable
                );
            } else {
                orderPage = orderRepository.findByMerchant_IdOrderByIdDesc(
                        merchantId,
                        pageable
                );
            }
        } else {
            if (status != null) {
                orderPage = orderRepository.searchOrdersByStatus(
                        merchantId,
                        status,
                        searchKeyword,
                        pageable
                );
            } else {
                orderPage = orderRepository.searchOrders(
                        merchantId,
                        searchKeyword,
                        pageable
                );
            }
        }

        for (Order order : orderPage.getContent()) {
            autoSyncOrderStatus(order);
        }

        return orderMapper.toOrderResponseDTOPage(orderPage);
    }

    /**
     * TÍNH NĂNG 2: Xem chi tiết một đơn hàng của Merchant
     */
    @Override
    @Transactional
    public OrderResponseDTO getMerchantOrderDetail(UUID merchantId, Long orderId) {
        Order order = orderRepository.findByIdAndMerchantIdWithItems(orderId, merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + orderId));

        autoSyncOrderStatus(order);
        return orderMapper.toOrderResponseDTO(order);
    }

    /**
     * TÍNH NĂNG 1: Merchant bấm "Nhận đơn" (Chuyển trạng thái sang PREPARING kèm mốc thời gian)
     */
    @Override
    @Transactional
    public OrderResponseDTO acceptOrder(UUID merchantId, Long orderId) {
        Order order = findMerchantOrderOrThrow(orderId, merchantId);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể nhận đơn hàng khi đơn đang ở trạng thái 'Chờ nhận hàng'");
        }

        LocalDateTime now = LocalDateTime.now();
        order.setStatus(OrderStatus.PREPARING);
        order.setAcceptedAt(now);

        // Tính thời gian chuẩn bị theo món ăn (mặc định 10 phút nếu không có)
        int prepMinutes = 10;
        if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
            int maxPrep = order.getOrderItems().stream()
                    .mapToInt(item -> (item.getFood() != null && item.getFood().getPreparationTime() != null
                            && item.getFood().getPreparationTime() > 0)
                            ? item.getFood().getPreparationTime()
                            : 10)
                    .max()
                    .orElse(10);
            prepMinutes = Math.max(5, maxPrep);
        }

        LocalDateTime preparingUntil = now.plusMinutes(prepMinutes);
        order.setPreparingUntil(preparingUntil);

        // Thời gian giao hàng dự kiến = thời gian chuẩn bị xong + thời gian vận chuyển (4 phút / 1km)
        int transitMinutes = calculateDeliveryTransitMinutes(order);
        order.setEstimatedDeliveryTime(preparingUntil.plusMinutes(transitMinutes));
        order.setUpdatedAt(now);

        Order savedOrder = orderRepository.save(order);
        log.info("Merchant {} đã nhận đơn hàng ID {}, chuẩn bị đến {}, dự kiến giao {}", merchantId, orderId, preparingUntil, order.getEstimatedDeliveryTime());

        return orderMapper.toOrderResponseDTO(savedOrder);
    }

    /**
     * Merchant bấm "Bắt đầu giao hàng" (Chuyển trạng thái sang DELIVERING)
     */
    @Override
    @Transactional
    public OrderResponseDTO startDelivery(UUID merchantId, Long orderId) {
        Order order = findMerchantOrderOrThrow(orderId, merchantId);

        if (order.getStatus() != OrderStatus.PREPARING) {
            throw new IllegalStateException("Chỉ có thể bắt đầu giao hàng khi đơn đang ở trạng thái 'Đang chuẩn bị'");
        }

        LocalDateTime now = LocalDateTime.now();
        order.setStatus(OrderStatus.DELIVERING);
        // Thời gian giao hàng ước tính tính từ lúc bắt đầu giao (4 phút / 1km)
        int transitMinutes = calculateDeliveryTransitMinutes(order);
        order.setEstimatedDeliveryTime(now.plusMinutes(transitMinutes));
        order.setUpdatedAt(now);

        Order savedOrder = orderRepository.save(order);
        log.info("Merchant {} đã bắt đầu giao đơn hàng ID {}, dự kiến giao {}", merchantId, orderId, order.getEstimatedDeliveryTime());

        return orderMapper.toOrderResponseDTO(savedOrder);
    }

    /**
     * TÍNH NĂNG 3: Merchant bấm "Hủy đơn" (Chuyển trạng thái sang CANCELLED)
     */
    @Override
    @Transactional
    public OrderResponseDTO cancelOrderByMerchant(UUID merchantId, Long orderId, String cancelReason) {
        Order order = findMerchantOrderOrThrow(orderId, merchantId);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể hủy đơn hàng khi đơn đang ở trạng thái 'Chờ nhận hàng'");
        }

        String reason = (cancelReason != null && !cancelReason.trim().isEmpty())
                ? cancelReason.trim()
                : "Cửa hàng đã hủy đơn";

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(reason);
        order.setUpdatedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);
        log.info("Merchant {} đã hủy đơn hàng ID {}, lý do: {}", merchantId, orderId, reason);

        return orderMapper.toOrderResponseDTO(savedOrder);
    }

    /**
     * Merchant bấm "Đã nhận tiền & Hoàn thành" (Chuyển trạng thái sang COMPLETED)
     */
    @Override
    @Transactional
    public OrderResponseDTO completeOrder(UUID merchantId, Long orderId) {
        Order order = findMerchantOrderOrThrow(orderId, merchantId);
        autoSyncOrderStatus(order);

        if (order.getStatus() != OrderStatus.PREPARING && order.getStatus() != OrderStatus.DELIVERING) {
            throw new IllegalStateException("Chỉ có thể hoàn thành đơn hàng khi đơn đang chuẩn bị hoặc đang giao");
        }

        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);
        log.info("Merchant {} đã xác nhận hoàn thành đơn hàng ID {}", merchantId, orderId);

        return orderMapper.toOrderResponseDTO(savedOrder);
    }

    /**
     * Merchant bấm "Khách không nhận hàng" (Chuyển trạng thái sang CANCELLED)
     */
    @Override
    @Transactional
    public OrderResponseDTO markFailedDelivery(UUID merchantId, Long orderId, String cancelReason) {
        Order order = findMerchantOrderOrThrow(orderId, merchantId);

        if (order.getStatus() != OrderStatus.PREPARING && order.getStatus() != OrderStatus.DELIVERING
                && order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Không thể ghi nhận hủy đơn hàng ở trạng thái hiện tại");
        }

        String reason = (cancelReason != null && !cancelReason.trim().isEmpty())
                ? cancelReason.trim()
                : "Khách không nhận hàng";

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(reason);
        order.setUpdatedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);
        log.info("Merchant {} xác nhận khách không nhận đơn ID {}, lý do: {}", merchantId, orderId, reason);

        return orderMapper.toOrderResponseDTO(savedOrder);
    }

    /**
     * Đếm số lượng đơn hàng đang chờ Merchant tiếp nhận (PENDING)
     */
    @Override
    @Transactional(readOnly = true)
    public long countPendingOrders(UUID merchantId) {
        return orderRepository.countByMerchant_IdAndStatus(merchantId, OrderStatus.PENDING);
    }

    // ==================== HELPER METHOD ====================

    private void autoSyncOrderStatus(Order order) {
        if (order == null)
            return;
        if (order.getStatus() == OrderStatus.PREPARING && order.getPreparingUntil() != null) {
            if (LocalDateTime.now().isAfter(order.getPreparingUntil())) {
                order.setStatus(OrderStatus.DELIVERING);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
                log.info("Tự động chuyển đơn hàng ID {} sang DELIVERING do hết thời gian chuẩn bị", order.getId());
            }
        }
    }

    private Order findMerchantOrderOrThrow(Long orderId, UUID merchantId) {
        return orderRepository.findByIdAndMerchant_Id(orderId, merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + orderId));
    }

    /**
     * Tính thời gian vận chuyển giao hàng dự kiến = 4 phút / 1km (tối thiểu 4 phút)
     */
    private int calculateDeliveryTransitMinutes(Order order) {
        double distanceKm = 3.0; // Mặc định 3km nếu không tính được (tương đương 12 phút)
        try {
            if (order != null && order.getMerchant() != null && order.getDeliveryAddress() != null) {
                List<MerchantAddress> merchantAddrs = merchantAddressRepository.findByMerchantId(order.getMerchant().getId());
                if (!merchantAddrs.isEmpty()) {
                    MerchantAddress mAddr = merchantAddrs.get(0);
                    GeoPoint mPoint = (mAddr.getLatitude() != null && mAddr.getLongitude() != null)
                            ? new GeoPoint(mAddr.getLatitude(), mAddr.getLongitude())
                            : geocodingService.geocode(mAddr.getMerchantAddress() + ", Việt Nam");
                    GeoPoint uPoint = geocodingService.geocode(order.getDeliveryAddress() + ", Việt Nam");
                    if (mPoint != null && uPoint != null) {
                        double d = distanceService.calculateDistanceKm(mPoint, uPoint);
                        if (order.getDeliveryAddress().toLowerCase().contains("hà nội")
                                && mAddr.getMerchantAddress().toLowerCase().contains("hà nội")
                                && d > 35.0) {
                            d = 3.0;
                        }
                        if (d > 0) {
                            distanceKm = d;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Không tính được khoảng cách đơn hàng ID {}, dùng mặc định 3km: {}", order != null ? order.getId() : null, e.getMessage());
        }
        // Thời gian giao hàng dự kiến: 4 phút / 1km
        return (int) Math.max(0.5, Math.round(distanceKm * 0.5));
    }
}