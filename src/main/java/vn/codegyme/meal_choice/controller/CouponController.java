package vn.codegyme.meal_choice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.codegyme.meal_choice.dto.coupon.CouponCreateRequest;
import vn.codegyme.meal_choice.dto.coupon.CouponResponse;
import vn.codegyme.meal_choice.dto.coupon.CouponUpdateRequest;
import vn.codegyme.meal_choice.entity.DiscountType;
import vn.codegyme.meal_choice.entity.Merchant;
import vn.codegyme.meal_choice.repository.MerchantRepository;
import vn.codegyme.meal_choice.security.CustomUserDetails;
import vn.codegyme.meal_choice.service.CouponService;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/merchant/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final MerchantRepository merchantRepository;

    @GetMapping
    public String list(Model model) {
        UUID merchantId = getCurrentMerchant().getId();

        model.addAttribute("coupons", couponService.getCoupons(merchantId));
        model.addAttribute("activeMenu", "coupons");

        return "merchant/coupon/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("coupon", new CouponCreateRequest());
        addCommonAttributes(model);

        return "merchant/coupon/create";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("coupon") CouponCreateRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("discountTypes", DiscountType.values());
            model.addAttribute("activeMenu", "coupons");
            return "merchant/coupon/create";
        }

        try {
            couponService.createCoupon(
                    request,
                    getCurrentMerchant().getId()
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Thêm coupon thành công."
            );

            return "redirect:/merchant/coupons";

        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("discountTypes", DiscountType.values());
            model.addAttribute("activeMenu", "coupons");

            return "merchant/coupon/create";
        }
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("coupon") CouponUpdateRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("couponId", id);
            addCommonAttributes(model);
            return "merchant/coupon/edit";
        }

        try {
            couponService.updateCoupon(
                    id,
                    request,
                    getCurrentMerchant().getId()
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Cập nhật coupon thành công."
            );

            return "redirect:/merchant/coupons";

        } catch (RuntimeException e) {
            model.addAttribute("couponId", id);
            model.addAttribute("error", e.getMessage());
            addCommonAttributes(model);
            return "merchant/coupon/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            couponService.deleteCoupon(
                    id,
                    getCurrentMerchant().getId()
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Xóa coupon thành công."
            );

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/merchant/coupons";
    }

    @GetMapping("/{id}/edit")
    public String editForm(
            @PathVariable Long id,
            Model model) {

        UUID merchantId = getCurrentMerchant().getId();

        CouponResponse coupon =
                couponService.getCoupon(id, merchantId);

        CouponUpdateRequest request = new CouponUpdateRequest();
        request.setCouponCode(coupon.getCouponCode());
        request.setDiscountType(coupon.getDiscountType());
        request.setDiscountValue(coupon.getDiscountValue());
        request.setStartAt(coupon.getStartAt());
        request.setEndAt(coupon.getEndAt());
        request.setUsageLimit(coupon.getUsageLimit());
        request.setIsActive(coupon.getIsActive());

        model.addAttribute("coupon", request);
        model.addAttribute("couponId", id);
        addCommonAttributes(model);

        return "merchant/coupon/edit";
    }

    @GetMapping("/{id}")
    public String detail(
            @PathVariable Long id,
            Model model) {

        UUID merchantId = getCurrentMerchant().getId();

        model.addAttribute(
                "coupon",
                couponService.getCoupon(id, merchantId)
        );

        model.addAttribute("activeMenu", "coupons");

        return "merchant/coupon/detail";
    }

    private void addCommonAttributes(Model model) {
        model.addAttribute("discountTypes", DiscountType.values());
        model.addAttribute("activeMenu", "coupons");
    }

    private Merchant getCurrentMerchant() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
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