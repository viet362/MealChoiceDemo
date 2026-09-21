package vn.codegyme.meal_choice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.codegyme.meal_choice.dto.order.CheckoutRequestDTO;
import vn.codegyme.meal_choice.dto.order.OrderResponseDTO;
import vn.codegyme.meal_choice.entity.OrderStatus;

import java.util.List;
import java.util.UUID;

/**
 * Service xử lý nghiệp vụ đặt hàng và lịch sử đơn của Khách hàng (User).
 */
public interface UserOrderService {

    /**
     * Tạo đơn hàng mới từ GIỎ HÀNG trên server.
     *
     * Danh sách món và giá tiền đều lấy từ bảng carts/cart_items và foods,
     * không tin dữ liệu client gửi lên. Giỏ hàng được dọn sạch sau khi đặt thành công.
     */
    OrderResponseDTO placeOrder(UUID userId, CheckoutRequestDTO request);

    /**
     * Danh sách lịch sử đơn hàng của User.
     */
    List<OrderResponseDTO> getUserOrders(UUID userId);

    /**
     * Danh sách lịch sử đơn hàng có phân trang, mới nhất trước.
     */
    Page<OrderResponseDTO> getUserOrders(UUID userId, Pageable pageable);

    /**
     * Danh sách đơn hàng lọc theo trạng thái, có phân trang.
     */
    Page<OrderResponseDTO> getUserOrders(UUID userId, OrderStatus status, Pageable pageable);

    /**
     * Chi tiết một đơn hàng theo ID, chỉ trả về nếu đơn thuộc về chính User đó.
     */
    OrderResponseDTO getUserOrderDetail(UUID userId, Long orderId);

    /**
     * Chi tiết đơn hàng theo mã đơn (trang Đặt hàng thành công).
     */
    OrderResponseDTO getOrderDetailByCode(String orderCode);

    /**
     * Chi tiết đơn hàng theo mã đơn, có kiểm tra chủ sở hữu.
     */
    OrderResponseDTO getOrderDetailByCode(String orderCode, UUID userId);

    /**
     * Khách hàng tự hủy đơn hàng.
     *
     * Chỉ cho phép khi đơn còn ở trạng thái PENDING (quán chưa bấm nhận đơn).
     * Lý do hủy là bắt buộc.
     */
    OrderResponseDTO cancelOrderByUser(UUID userId, Long orderId, String cancelReason);

    /**
     * Kiểm tra một đơn hàng có đang được phép hủy hay không.
     */
    boolean isCancellableByUser(UUID userId, Long orderId);
}