package vn.codegyme.meal_choice.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.codegyme.meal_choice.entity.Merchant;
import vn.codegyme.meal_choice.entity.User;
import vn.codegyme.meal_choice.repository.MerchantRepository;
import vn.codegyme.meal_choice.repository.UserRepository;
import vn.codegyme.meal_choice.service.MerchantFinanceService;
import vn.codegyme.meal_choice.service.MerchantPayoutService;

import java.math.BigDecimal;

@Controller
@RequestMapping("/merchant/payout")
@RequiredArgsConstructor
public class MerchantPayoutController {

    private final MerchantRepository merchantRepository;

    private final UserRepository userRepository;

    private final MerchantFinanceService merchantFinanceService;

    private final MerchantPayoutService merchantPayoutService;


    // =====================================================
    // TRANG RÚT TIỀN
    // =====================================================

    @GetMapping("/withdraw")
    public String withdrawPage(
            Authentication authentication,
            Model model
    ) {
        System.out.println("========== WITHDRAW START ==========");

        Merchant merchant =
                getCurrentMerchant(authentication);
        System.out.println(
                "1. Merchant OK: " + merchant.getId());


        // Tổng doanh thu tích lũy
        BigDecimal totalRevenue =
                merchantFinanceService
                        .getTotalRevenue(
                                merchant.getId()
                        );
        System.out.println(
                "2. Total revenue OK: " + totalRevenue
        );


        // Tổng số tiền Admin đã thanh toán
        BigDecimal totalPaidAmount =
                merchantFinanceService
                        .getTotalPaidAmount(
                                merchant.getId()
                        );
        System.out.println(
                "3. Total paid OK: " + totalPaidAmount
        );


        // Số dư còn có thể rút
        BigDecimal availableBalance =
                merchantFinanceService
                        .getAvailableBalance(
                                merchant.getId()
                        );
        System.out.println(
                "4. Available balance OK: "
                        + availableBalance
        );
        System.out.println(
                "5. Loading payout history..."
        );

        var requests =
                merchantPayoutService
                        .getMerchantRequests(
                                merchant.getId()
                        );

        var transactions =
                merchantPayoutService
                        .getTransactionHistory(
                                merchant.getId()
                        );

        model.addAttribute(
                "requests",
                requests
        );

        model.addAttribute(
                "transactions",
                transactions
        );

        model.addAttribute(
                "merchant",
                merchant
        );


        model.addAttribute(
                "totalRevenue",
                totalRevenue
        );


        model.addAttribute(
                "totalPaidAmount",
                totalPaidAmount
        );


        model.addAttribute(
                "availableBalance",
                availableBalance
        );


        // Tổng số tiền đang chờ Admin duyệt
        BigDecimal totalPendingAmount =
                merchantFinanceService
                        .getTotalPendingAmount(
                                merchant.getId()
                        );

        model.addAttribute(
                "totalPendingAmount",
                totalPendingAmount
        );

        // Kiểm tra đủ điều kiện: Doanh thu đã đối soát > 1000 và Số dư khả dụng > 0
        boolean canWithdraw =
                totalRevenue.compareTo(
                        new BigDecimal("1000")
                ) > 0 && availableBalance.compareTo(BigDecimal.ZERO) > 0;

        model.addAttribute(
                "canWithdraw",
                canWithdraw
        );


        // Lịch sử yêu cầu của Merchant
        model.addAttribute(
                "requests",
                merchantPayoutService
                        .getMerchantRequests(
                                merchant.getId()
                        )
        );
        System.out.println(
                "7. RETURN withdraw.html");


        return "merchant/payout/withdraw";
    }


    // =====================================================
    // GỬI YÊU CẦU RÚT TIỀN
    // =====================================================

    @PostMapping("/withdraw")
    public String withdraw(
            @RequestParam BigDecimal amount,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {

        try {

            Merchant merchant =
                    getCurrentMerchant(
                            authentication
                    );


            merchantPayoutService
                    .createWithdrawalRequest(
                            merchant.getId(),
                            amount
                    );


            redirectAttributes
                    .addFlashAttribute(
                            "message",
                            "Đã gửi yêu cầu rút tiền đến Admin."
                    );


        } catch (Exception e) {

            redirectAttributes
                    .addFlashAttribute(
                            "error",
                            e.getMessage()
                    );
        }


        return "redirect:/merchant/payout/withdraw";
    }


    // =====================================================
    // TRANG THANH LÝ HỢP ĐỒNG
    // =====================================================

    @GetMapping("/liquidation")
    public String liquidationPage(
            Authentication authentication,
            Model model
    ) {

        Merchant merchant =
                getCurrentMerchant(
                        authentication
                );


        BigDecimal totalRevenue =
                merchantFinanceService
                        .getTotalRevenue(
                                merchant.getId()
                        );


        BigDecimal totalPaidAmount =
                merchantFinanceService
                        .getTotalPaidAmount(
                                merchant.getId()
                        );


        BigDecimal availableBalance =
                merchantFinanceService
                        .getAvailableBalance(
                                merchant.getId()
                        );


        model.addAttribute(
                "merchant",
                merchant
        );


        model.addAttribute(
                "totalRevenue",
                totalRevenue
        );


        model.addAttribute(
                "totalPaidAmount",
                totalPaidAmount
        );


        model.addAttribute(
                "availableBalance",
                availableBalance
        );


        // Thanh lý không cần điều kiện doanh thu > 100 triệu
        boolean canLiquidate =
                availableBalance.compareTo(
                        BigDecimal.ZERO
                ) > 0;


        model.addAttribute(
                "canLiquidate",
                canLiquidate
        );


        // Lịch sử yêu cầu
        model.addAttribute(
                "requests",
                merchantPayoutService
                        .getMerchantRequests(
                                merchant.getId()
                        )
        );


        return "merchant/payout/liquidation";
    }


    // =====================================================
    // GỬI YÊU CẦU THANH LÝ
    // =====================================================

    @PostMapping("/liquidation")
    public String liquidation(
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {

        try {

            Merchant merchant =
                    getCurrentMerchant(
                            authentication
                    );


            merchantPayoutService
                    .createLiquidationRequest(
                            merchant.getId()
                    );


            redirectAttributes
                    .addFlashAttribute(
                            "message",
                            "Đã gửi yêu cầu thanh lý hợp đồng đến Admin."
                    );


        } catch (Exception e) {

            redirectAttributes
                    .addFlashAttribute(
                            "error",
                            e.getMessage()
                    );
        }


        return "redirect:/merchant/payout/liquidation";
    }


    // =====================================================
    // LẤY MERCHANT HIỆN ĐANG ĐĂNG NHẬP
    // =====================================================

    private Merchant getCurrentMerchant(
            Authentication authentication
    ) {

        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new IllegalStateException(
                    "Bạn chưa đăng nhập."
            );
        }


        String email =
                authentication.getName();


        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Không tìm thấy tài khoản người dùng."
                                )
                        );


        return merchantRepository
                .findByUser_Id(
                        user.getId()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Tài khoản hiện tại không phải Merchant."
                        )
                );
    }
}
