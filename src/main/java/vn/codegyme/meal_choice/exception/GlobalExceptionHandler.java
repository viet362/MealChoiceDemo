package vn.codegyme.meal_choice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import vn.codegyme.meal_choice.controller.FoodController;

import java.util.stream.Collectors;

/**
 * Bắt lỗi cho các endpoint trả JSON.
 *
 * LƯU Ý VỀ PHẠM VI:
 * Trước đây advice chỉ khai báo annotations = RestController.class, trong khi
 * FoodController lại được đánh dấu @Controller (vì nó vừa trả JSON qua
 * @ResponseBody, vừa render trang chi tiết món ăn). Hệ quả là mọi lỗi từ
 * /api/merchant/foods không bao giờ đi qua đây, client nhận về body lỗi mặc
 * định của Spring và không có trường "message", nên giao diện chỉ hiện được
 * thông báo dự phòng chung chung kiểu "Không thể thêm món ăn".
 *
 * assignableTypes bổ sung FoodController vào phạm vi. Hai thuộc tính này là
 * phép HỢP: bean nào thỏa một trong hai đều được advice xử lý.
 */
@RestControllerAdvice(
        annotations = RestController.class,
        assignableTypes = { FoodController.class }
)
public class GlobalExceptionHandler {

    // ==================== UPLOAD ====================

    /**
     * Ảnh vượt quá spring.servlet.multipart.max-file-size.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ErrorResponse(
                        HttpStatus.PAYLOAD_TOO_LARGE.value(),
                        "Ảnh tải lên quá lớn. Chọn ảnh dưới 10MB hoặc giảm số ảnh trong một lần tải."));
    }

    // ==================== VALIDATION ====================

    /**
     * Lỗi validate trên @RequestBody.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), collectFieldErrors(e)));
    }

    /**
     * Lỗi validate trên @ModelAttribute (form multipart, ví dụ form thêm món ăn).
     *
     * Spring ném BindException chứ không phải MethodArgumentNotValidException,
     * và MethodArgumentNotValidException lại là lớp CON của BindException, nên
     * handler ở trên không bắt được trường hợp này. Đây là lý do form thêm món
     * ăn báo lỗi mà không kèm nội dung cụ thể.
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), collectFieldErrors(e)));
    }

    /**
     * Thiếu tham số bắt buộc, ví dụ chưa chọn ảnh nào cho @RequestParam("images").
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException e) {

        String message = "images".equals(e.getParameterName())
                ? "Vui lòng chọn ảnh cho món ăn."
                : "Thiếu thông tin bắt buộc: " + e.getParameterName();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message));
    }

    /**
     * Sai kiểu dữ liệu, ví dụ giá tiền không phải số hoặc ID không đúng định dạng.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        "Giá trị của trường \"" + e.getName() + "\" không hợp lệ."));
    }

    // ==================== NGHIỆP VỤ ====================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), safeMessage(e)));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException e) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), safeMessage(e)));
    }

    // ==================== XÁC THỰC & PHÂN QUYỀN ====================

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException e) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), safeMessage(e)));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(HttpStatus.FORBIDDEN.value(), safeMessage(e)));
    }

    // ==================== RUNTIME CHUNG ====================

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {

        String message = safeMessage(e);

        // Lỗi liên quan tới Refresh Token hoặc trạng thái kích hoạt tài khoản -> 401
        if (message.contains("Refresh token")
                || message.contains("hết hạn")
                || message.contains("kích hoạt")) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), message));
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message));
    }

    // ==================== HELPER ====================

    private String collectFieldErrors(BindException e) {

        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .filter(m -> m != null && !m.isBlank())
                .distinct()
                .collect(Collectors.joining("; "));

        return message.isBlank() ? "Dữ liệu gửi lên không hợp lệ." : message;
    }

    private String safeMessage(Exception e) {
        return (e.getMessage() != null && !e.getMessage().isBlank())
                ? e.getMessage()
                : "Đã xảy ra lỗi, vui lòng thử lại.";
    }
}