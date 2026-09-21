package vn.codegyme.meal_choice.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.codegyme.meal_choice.event.UserRegisteredEvent;
import vn.codegyme.meal_choice.service.EmailService;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    /**
     * Lắng nghe UserRegisteredEvent và chỉ gửi mail SAU KHI transaction đăng ký
     * (AuthService#register) đã commit thành công (AFTER_COMMIT).
     * -> Nếu đăng ký thất bại/rollback (vd trùng email) thì sẽ KHÔNG gửi mail.
     * -> @Async đảm bảo việc gửi mail chạy trên thread riêng (mailTaskExecutor
     *    khai báo trong MailConfig), không làm chậm response trả về cho client.
     */
    @Async("mailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        sendActivationEmail(event.email(), event.displayName(), event.activationLink(), event.expirationMinutes());
    }

    @Async("mailTaskExecutor")
    public void sendMerchantRegisterEmail(String email, String restaurantName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();

            message.setTo(email);
            message.setSubject("Đăng ký Merchant thành công");
            message.setText(
                    "Xin chào,\n\n" +
                            "Nhà hàng " + restaurantName +
                            " đã đăng ký Merchant thành công trên hệ thống Trưa Nay Ăn Gì.\n\n" +
                            "Hồ sơ của bạn đang chờ Admin xét duyệt.\n\n" +
                            "Trân trọng."
            );

            mailSender.send(message);
        } catch (MailException ex) {
            log.error("Gửi email xác nhận Merchant tới '{}' thất bại: {}", email, ex.getMessage(), ex);
        }
    }

    /**
     * Gửi email đăng ký thành công kèm link kích hoạt tài khoản (email HTML).
     */
    public void sendActivationEmail(String email, String displayName, String activationLink, long expirationMinutes) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            helper.setTo(email);
            helper.setSubject("Kích hoạt tài khoản Trưa Nay Ăn Gì");

            String htmlContent = buildActivationEmailHtml(displayName, activationLink, expirationMinutes);
            helper.setText(htmlContent, true); // true = nội dung là HTML

            mailSender.send(mimeMessage);
            log.info("Đã gửi email kích hoạt tới '{}'", email);
        } catch (MessagingException | MailException ex) {
            // Không throw lại: đây là tác vụ phụ, không được phép ảnh hưởng
            // tới luồng nghiệp vụ chính (đăng ký tài khoản đã commit xong rồi).
            log.error("Gửi email kích hoạt tới '{}' thất bại: {}", email, ex.getMessage(), ex);
        }
    }

    private String buildActivationEmailHtml(String displayName, String activationLink, long expirationMinutes) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background-color:#f4f4f7;font-family:Arial,Helvetica,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="padding:24px 0;">
                    <tr>
                      <td align="center">
                        <table width="480" cellpadding="0" cellspacing="0"
                               style="background:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 2px 6px rgba(0,0,0,0.08);">
                          <tr>
                            <td style="background:#ff6b35;padding:20px 32px;">
                              <h1 style="color:#ffffff;font-size:20px;margin:0;">Trưa Nay Ăn Gì</h1>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px;">
                              <p style="font-size:15px;color:#333333;margin:0 0 12px 0;">Xin chào <b>%s</b>,</p>
                              <p style="font-size:15px;color:#333333;line-height:1.5;margin:0 0 24px 0;">
                                Cảm ơn bạn đã đăng ký tài khoản. Vui lòng bấm vào nút bên dưới để
                                kích hoạt tài khoản trước khi đăng nhập. Link có hiệu lực trong
                                <b>%d phút</b>.
                              </p>
                              <table cellpadding="0" cellspacing="0" style="margin:0 auto 24px auto;">
                                <tr>
                                  <td style="border-radius:6px;background:#ff6b35;">
                                    <a href="%s"
                                       style="display:inline-block;padding:12px 28px;font-size:15px;
                                              color:#ffffff;text-decoration:none;font-weight:bold;">
                                      Kích hoạt tài khoản
                                    </a>
                                  </td>
                                </tr>
                              </table>
                              <p style="font-size:13px;color:#888888;line-height:1.5;margin:0;">
                                Nếu nút bấm không hoạt động, hãy sao chép đường dẫn sau vào trình duyệt:<br>
                                <a href="%s" style="color:#ff6b35;word-break:break-all;">%s</a>
                              </p>
                              <p style="font-size:13px;color:#888888;margin:24px 0 0 0;">
                                Nếu bạn không thực hiện đăng ký này, vui lòng bỏ qua email.
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(displayName, expirationMinutes, activationLink, activationLink, activationLink);
    }

    @Override
    public void sendTrustedPartnerRegistrationEmail(
            String email,
            String restaurantName
    ) {
        sendEmail(
                email,
                "Đăng ký đối tác thân thiết thành công",
                "Xin chào,\n\n" +
                        "Nhà hàng " + restaurantName +
                        " đã đăng ký trở thành đối tác thân thiết trên hệ thống Trưa Nay Ăn Gì.\n\n" +
                        "Hồ sơ đăng ký của bạn đang chờ Admin xét duyệt.\n\n" +
                        "Vui lòng chờ thông báo từ hệ thống sau khi Admin hoàn tất xét duyệt.\n\n" +
                        "Trân trọng."
        );
    }

    @Override
    public void sendTrustedPartnerApprovedEmail(
            String email,
            String restaurantName
    ) {
        sendEmail(
                email,
                "Đăng ký đối tác thân thiết được phê duyệt",
                "Xin chào,\n\n" +
                        "Chúc mừng nhà hàng " + restaurantName +
                        " đã được Admin phê duyệt trở thành đối tác thân thiết trên hệ thống Trưa Nay Ăn Gì.\n\n" +
                        "Từ bây giờ, nhà hàng của bạn chính thức là đối tác thân thiết.\n\n" +
                        "Trân trọng."
        );
    }

    @Override
    public void sendTrustedPartnerRejectedEmail(
            String email,
            String restaurantName,
            String reason
    ) {
        sendEmail(
                email,
                "Đăng ký đối tác thân thiết bị từ chối",
                "Xin chào,\n\n" +
                        "Đăng ký trở thành đối tác thân thiết của nhà hàng " +
                        restaurantName +
                        " chưa được Admin phê duyệt.\n\n" +
                        "Lý do: " + reason + "\n\n" +
                        "Bạn có thể đăng ký lại sau khi đáp ứng đủ điều kiện.\n\n" +
                        "Trân trọng."
        );
    }

    private void sendEmail(
            String email,
            String subject,
            String text
    ) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }
}
