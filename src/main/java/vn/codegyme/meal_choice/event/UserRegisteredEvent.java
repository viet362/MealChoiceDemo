package vn.codegyme.meal_choice.event;

/**
 * Sự kiện được publish khi một User đăng ký tài khoản thành công.
 * Chỉ chứa dữ liệu nguyên thủy (không chứa entity) để an toàn khi
 * xử lý bất đồng bộ trên thread khác.
 */
public record UserRegisteredEvent(String email, String displayName, String activationLink, long expirationMinutes) {
}
