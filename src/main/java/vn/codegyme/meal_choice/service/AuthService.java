package vn.codegyme.meal_choice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegyme.meal_choice.dto.AuthResponse;
import vn.codegyme.meal_choice.dto.LoginRequest;
import vn.codegyme.meal_choice.dto.RegisterRequest;
import vn.codegyme.meal_choice.entity.ActivationToken;
import vn.codegyme.meal_choice.entity.RefreshToken;
import vn.codegyme.meal_choice.entity.Role;
import vn.codegyme.meal_choice.entity.User;
import vn.codegyme.meal_choice.event.UserRegisteredEvent;
import vn.codegyme.meal_choice.repository.ActivationTokenRepository;
import vn.codegyme.meal_choice.repository.RefreshTokenRepository;
import vn.codegyme.meal_choice.repository.RoleRepository;
import vn.codegyme.meal_choice.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ActivationTokenRepository activationTokenRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.activation.expiration-minutes:15}")
    private long activationExpirationMinutes;

    private static final long REFRESH_TOKEN_EXPIRATION_MS = 604800000L;

    @Transactional
    public String register(RegisterRequest registerRequest) {

        // Check email if it exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }

        // Check phone number if it exists
        if (userRepository.existsByPhoneNumber(registerRequest.getPhoneNumber())) {
            throw new RuntimeException("Số điện thoại đã tồn tại");
        }

        // Check duplicate password
        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp");
        }

        Role userRole = roleRepository.findByName(Role.RoleName.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Role USER chưa được khởi tạo trong hệ thống"));

        // Create a new user with hash password. Tài khoản chưa được kích hoạt
        // (isActive = false) cho tới khi bấm link kích hoạt gửi qua email.
        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setDisplayName(registerRequest.getDisplayName());
        user.setRoles(new HashSet<>(Set.of(userRole)));
        user.setIsActive(false);

        // Save in database
        userRepository.save(user);

        // Sinh token kích hoạt tài khoản
        String rawToken = UUID.randomUUID().toString();
        ActivationToken activationToken = new ActivationToken();
        activationToken.setToken(rawToken);
        activationToken.setUser(user);
        activationToken.setExpiryDate(LocalDateTime.now().plusMinutes(activationExpirationMinutes));
        activationTokenRepository.save(activationToken);

        String activationLink = baseUrl + "/activate?token=" + rawToken;

        // Phát sự kiện đăng ký thành công. EmailService sẽ chỉ gửi mail
        // SAU KHI transaction này commit thành công (xem EmailService#onUserRegistered).
        eventPublisher.publishEvent(
                new UserRegisteredEvent(user.getEmail(), user.getDisplayName(), activationLink, activationExpirationMinutes));

        return "Đăng ký thành công! Vui lòng kiểm tra email để kích hoạt tài khoản.";
    }

    @Transactional
    public void activateAccount(String rawToken) {
        ActivationToken activationToken = activationTokenRepository.findByToken(rawToken)
                .orElseThrow(() -> new RuntimeException("Link kích hoạt không hợp lệ"));

        if (activationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            activationTokenRepository.delete(activationToken);
            throw new RuntimeException("Link kích hoạt đã hết hạn. Vui lòng yêu cầu gửi lại email kích hoạt.");
        }

        User user = activationToken.getUser();

        if (Boolean.TRUE.equals(user.getIsActive())) {
            activationTokenRepository.delete(activationToken);
            throw new RuntimeException("Tài khoản đã được kích hoạt trước đó");
        }

        user.setIsActive(true);
        userRepository.save(user);

        // Token chỉ dùng được một lần
        activationTokenRepository.delete(activationToken);
    }

    @Transactional
    public AuthResponse login(LoginRequest loginRequest) {

        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("Email hoặc mật khẩu không đúng"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new RuntimeException("Email hoặc mật khẩu không đúng");
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new RuntimeException("Tài khoản chưa được kích hoạt. Vui lòng kiểm tra email để kích hoạt tài khoản.");
        }

        return buildAuthResponse(user);
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new RuntimeException("Refresh token không hợp lệ"));

        refreshTokenRepository.delete(refreshToken);
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenValue) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new RuntimeException("Refresh token không hợp lệ"));

        if (storedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(storedToken);
            throw new RuntimeException("Refresh token đã hết hạn, vui lòng đăng nhập lại");
        }

        User user = storedToken.getUser();
        String newAccessToken = jwtService.generateAccessToken(user.getEmail());

        AuthResponse response = new AuthResponse();
        response.setAccessToken(newAccessToken);
        response.setRefreshToken(refreshTokenValue); // giữ nguyên, không cấp lại
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setDisplayName(user.getDisplayName());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setRoles(
                user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet()));
        return response;
    }

    private AuthResponse buildAuthResponse(User user) {

        String accessToken = jwtService.generateAccessToken(user.getEmail());
        String refreshTokenValue = jwtService.generateRefreshToken(user.getEmail());

        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(refreshTokenValue);
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(LocalDateTime.now().plusNanos(REFRESH_TOKEN_EXPIRATION_MS * 1_000_000));
        refreshTokenRepository.save(refreshToken);

        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken(accessToken);
        authResponse.setRefreshToken(refreshTokenValue);
        authResponse.setId(user.getId());
        authResponse.setEmail(user.getEmail());
        authResponse.setDisplayName(user.getDisplayName());
        authResponse.setPhoneNumber(user.getPhoneNumber());
        authResponse.setAvatarUrl(user.getAvatarUrl());
        authResponse.setRoles(
                user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet()));

        return authResponse;
    }
}
