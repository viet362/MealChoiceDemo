package vn.codegyme.meal_choice.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.codegyme.meal_choice.dto.*;
import vn.codegyme.meal_choice.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        String message = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse(message));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse httpServletResponse) {
        AuthResponse response = authService.login(loginRequest);

        // Thiết lập Cookie accessToken chuẩn từ Server
        if (response.getAccessToken() != null) {
            Cookie cookie = new Cookie("accessToken", response.getAccessToken());
            cookie.setPath("/");
            cookie.setMaxAge(86400); // 1 ngày
            cookie.setHttpOnly(false);
            httpServletResponse.addCookie(cookie);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody(required = false) LogoutRequest request, HttpServletResponse httpServletResponse) {
        if (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank()) {
            try {
                authService.logout(request.getRefreshToken());
            } catch (Exception ignored) {
            }
        }
        // Xóa cookie accessToken khi đăng xuất
        Cookie cookie = new Cookie("accessToken", "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        httpServletResponse.addCookie(cookie);

        return ResponseEntity.noContent().build();
    }

    @PostMapping({"/refresh-token", "/refresh"})
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody LogoutRequest request, HttpServletResponse httpServletResponse) {
        AuthResponse response = authService.refreshToken(request.getRefreshToken());

        if (response.getAccessToken() != null) {
            Cookie cookie = new Cookie("accessToken", response.getAccessToken());
            cookie.setPath("/");
            cookie.setMaxAge(86400);
            cookie.setHttpOnly(false);
            httpServletResponse.addCookie(cookie);
        }

        return ResponseEntity.ok(response);
    }
}
