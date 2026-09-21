package vn.codegyme.meal_choice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.codegyme.meal_choice.entity.Merchant;
import vn.codegyme.meal_choice.entity.MerchantStatus;
import vn.codegyme.meal_choice.entity.User;
import vn.codegyme.meal_choice.repository.MerchantRepository;
import vn.codegyme.meal_choice.repository.UserRepository;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class MerchantBlockedFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final MerchantRepository merchantRepository;


    /**
     * Chỉ kiểm tra các request đi vào kênh quản trị Merchant.
     */
    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {

        String uri = request.getRequestURI();

        /*
         * Các endpoint đăng ký, kiểm tra trạng thái và API công khai cho khách hàng:
         */
        if (uri.equals("/merchant/register") 
                || uri.equals("/api/merchant/register")
                || uri.equals("/api/merchant/my-status")
                || uri.startsWith("/api/merchants/")) {
            return true;
        }

        /*
         * Chỉ chạy Filter với các kênh quản lý nội bộ của Merchant:
         * /merchant/**
         * /api/merchant/**
         */
        return !uri.startsWith("/merchant/")
                && !uri.startsWith("/api/merchant/");
    }


    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();


        // =========================================
        // 1. CHƯA ĐĂNG NHẬP
        // =========================================

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(
                authentication.getPrincipal()
        )) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }


        // =========================================
        // 2. LẤY EMAIL USER ĐANG ĐĂNG NHẬP
        // =========================================

        String email =
                authentication.getName();


        // =========================================
        // 3. TÌM USER
        // =========================================

        User user = userRepository
                .findByEmail(email)
                .orElse(null);


        if (user == null) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }


        // =========================================
        // 4. TÌM MERCHANT CỦA USER
        // =========================================

        Merchant merchant = merchantRepository
                .findByUser_Id(user.getId())
                .orElse(null);


        // =========================================
        // 5. USER KHÔNG CÓ MERCHANT
        // =========================================

        if (merchant == null) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }


        // =========================================
        // 6. MERCHANT BỊ KHÓA
        // =========================================

        if (merchant.getMerchantStatus()
                == MerchantStatus.BLOCKED) {

            String uri = request.getRequestURI();
            /*
             * API thì trả về JSON FORBIDDEN.
             */
            if (uri.startsWith("/api/merchant/") || uri.startsWith("/api/merchants/")) {

                response.setStatus(
                        HttpServletResponse.SC_FORBIDDEN
                );

                response.setContentType(
                        "application/json;charset=UTF-8"
                );

                response.getWriter().write(
                        """
                        {
                          "error": "MERCHANT_BLOCKED",
                          "message": "Merchant đã bị khóa."
                        }
                        """
                );

                return;
            }


            /*
             * Trang Thymeleaf:
             * chuyển sang trang thông báo bị khóa.
             */
            response.sendRedirect(
                    request.getContextPath()
                            + "/home?merchantBlocked=true"
            );

            return;
        }


        // =========================================
        // 7. MERCHANT KHÔNG BỊ KHÓA
        // =========================================

        filterChain.doFilter(
                request,
                response
        );
    }
}
