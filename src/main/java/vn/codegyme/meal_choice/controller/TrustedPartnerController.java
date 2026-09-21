package vn.codegyme.meal_choice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.codegyme.meal_choice.entity.Merchant;
import vn.codegyme.meal_choice.entity.OrderStatus;
import vn.codegyme.meal_choice.entity.TrustedPartnerRequest;
import vn.codegyme.meal_choice.repository.MerchantRepository;
import vn.codegyme.meal_choice.repository.OrderRepository;
import vn.codegyme.meal_choice.repository.TrustedPartnerRequestRepository;
import vn.codegyme.meal_choice.security.CustomUserDetails;
import vn.codegyme.meal_choice.service.TrustedPartnerService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Optional;

@Controller
@RequestMapping("/merchant/trusted-partner")
@RequiredArgsConstructor
public class TrustedPartnerController {

    private static final BigDecimal MIN_REVENUE =
            new BigDecimal("100000000");

    private final MerchantRepository merchantRepository;
    private final OrderRepository orderRepository;
    private final TrustedPartnerService trustedPartnerService;
    private final TrustedPartnerRequestRepository trustedPartnerRequestRepository;

    @GetMapping
    public String showPage(Model model) {
        Merchant merchant = getCurrentMerchant();

        YearMonth currentMonth = YearMonth.now();

        LocalDateTime startDate = currentMonth
                .atDay(1)
                .atStartOfDay();

        LocalDateTime endDate = currentMonth
                .plusMonths(1)
                .atDay(1)
                .atStartOfDay();

        BigDecimal monthlyRevenue = orderRepository.calculateRevenue(
                merchant.getId(),
                OrderStatus.COMPLETED,
                startDate,
                endDate
        );
        if (monthlyRevenue == null) {
            monthlyRevenue = BigDecimal.ZERO;
        }

        Optional<TrustedPartnerRequest> request =
                trustedPartnerRequestRepository
                        .findFirstByMerchant_IdOrderByCreatedAtDesc(
                                merchant.getId()
                        );

        model.addAttribute("merchant", merchant);
        model.addAttribute("monthlyRevenue", monthlyRevenue);
        model.addAttribute(
                "eligible",
                monthlyRevenue.compareTo(MIN_REVENUE) >= 0
        );
        model.addAttribute(
                "trustedPartnerRequest",
                request.orElse(null)
        );
        model.addAttribute("activeMenu", "trusted-partner");

        return "merchant/trusted-partner";
    }

    @PostMapping("/register")
    public String register() {
        Merchant merchant = getCurrentMerchant();

        trustedPartnerService.registerTrustedPartner(
                merchant.getId()
        );

        return "redirect:/merchant/trusted-partner";
    }

    private Merchant getCurrentMerchant() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof CustomUserDetails userDetails)) {

            throw new RuntimeException(
                    "Người dùng chưa đăng nhập hoặc phiên làm việc không hợp lệ"
            );
        }

        return merchantRepository.findByUser_Id(userDetails.getId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy thông tin Merchant của tài khoản"
                        )
                );
    }
}