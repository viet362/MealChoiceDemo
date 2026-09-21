package vn.codegyme.meal_choice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import vn.codegyme.meal_choice.entity.Role;
import vn.codegyme.meal_choice.entity.User;
import vn.codegyme.meal_choice.repository.RoleRepository;
import vn.codegyme.meal_choice.repository.UserRepository;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        String adminEmail = "lanh123ngoc@gmail.com";

        // Nếu Admin đã tồn tại thì không tạo lại
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        // Lấy ROLE_ADMIN, nếu chưa có thì tạo
        Role adminRole = roleRepository.findByName(Role.RoleName.ROLE_ADMIN)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(Role.RoleName.ROLE_ADMIN);
                    return roleRepository.save(role);
                });

        // Tạo tài khoản Admin
        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setPhoneNumber("0900000000");
        admin.setDisplayName("Administrator");
        admin.setRoles(new HashSet<>(Set.of(adminRole)));

        userRepository.save(admin);

        System.out.println("========================================");
        System.out.println("Đã tạo tài khoản ADMIN");
        System.out.println("Email: " + adminEmail);
        System.out.println("Password: admin123");
        System.out.println("========================================");
    }
}