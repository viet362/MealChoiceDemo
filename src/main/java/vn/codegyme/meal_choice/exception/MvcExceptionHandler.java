package vn.codegyme.meal_choice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.codegyme.meal_choice.controller.MerchantPayoutController;
import vn.codegyme.meal_choice.controller.PageController;
import vn.codegyme.meal_choice.controller.TrustedPartnerController;

/**
 * Xử lý exception cho các trang Thymeleaf (MVC controllers).
 * Ngăn chặn Whitelabel Error Page khi có lỗi xảy ra trong
 * TrustedPartnerController, MerchantPayoutController, PageController, v.v.
 */
@ControllerAdvice(
        assignableTypes = {
                TrustedPartnerController.class,
                MerchantPayoutController.class,
                PageController.class
        }
)
public class MvcExceptionHandler {

    /**
     * Bắt mọi exception từ MVC controllers render Thymeleaf.
     * Redirect về trang trước (hoặc dashboard) kèm thông báo lỗi.
     */
    @ExceptionHandler(Exception.class)
    public String handleMvcException(
            Exception ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        String message = (ex.getMessage() != null && !ex.getMessage().isBlank())
                ? ex.getMessage()
                : "Đã xảy ra lỗi, vui lòng thử lại.";

        redirectAttributes.addFlashAttribute("error", message);

        // Lấy Referer để redirect về trang trước nếu có
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            return "redirect:" + referer;
        }

        // Mặc định redirect về merchant dashboard
        return "redirect:/merchant/dashboard";
    }
}
