package vn.codegyme.meal_choice.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import vn.codegyme.meal_choice.entity.PaymentMethod;

import java.util.List;
import java.util.UUID;

/**
 * Dữ liệu người dùng gửi lên khi bấm Đặt hàng.
 *
 * Danh sách món KHÔNG còn lấy từ đây nữa. Server luôn đọc món và giá từ
 * giỏ hàng trong database, nên client không thể tự đặt giá hoặc thêm món lạ.
 *
 * merchantId và items chỉ dùng để đối chiếu: nếu client gửi lên mà lệch với
 * giỏ hàng thật thì server báo lỗi để người dùng tải lại trang.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutRequestDTO {

    /**
     * Tùy chọn. Nếu có, phải khớp với Merchant của giỏ hàng hiện tại.
     */
    private UUID merchantId;

    @NotBlank(message = "Tên người nhận không được để trống")
    private String contactName;

    @NotBlank(message = "Số điện thoại nhận hàng không được để trống")
    private String contactPhone;

    @NotBlank(message = "Địa chỉ nhận hàng không được để trống")
    private String deliveryAddress;

    /**
     * ID địa chỉ đã lưu (Address entity). Nếu có, backend dùng tọa độ lat/lon
     * đã lưu trong DB (giống DeliveryQuoteService) để tính phí ship chính xác,
     * thay vì phải geocode lại từ text gây ra sự chênh lệch.
     */
    private Long addressId;

    private String note;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentMethod paymentMethod;

    private String voucherCode;

    private UUID deliveryPartnerId;

    /**
     * Tùy chọn, chỉ để đối chiếu với giỏ hàng trên server.
     */
    @Valid
    private List<CheckoutItemDTO> items;
}