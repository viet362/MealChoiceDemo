package vn.codegyme.meal_choice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.codegyme.meal_choice.dto.food.FoodResponse;
import vn.codegyme.meal_choice.entity.Merchant;
import vn.codegyme.meal_choice.repository.*;
import vn.codegyme.meal_choice.security.CustomUserDetails;
import vn.codegyme.meal_choice.service.FoodService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.UUID;

@Controller
@RequestMapping("/merchant/foods")
@RequiredArgsConstructor
public class FoodViewController {

    private final FoodService foodService;
    private final MerchantRepository merchantRepository;
    private final MerchantAddressRepository merchantAddressRepository;
    private final FoodCategoryRepository foodCategoryRepository;
    private final TagRepository tagRepository;
    private final CouponRepository couponRepository;

    private Merchant getCurrentMerchant() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new RuntimeException("Người dùng chưa đăng nhập hoặc phiên làm việc không hợp lệ");
        }

        UUID userId = userDetails.getId();

        return merchantRepository.findByUser_Id(userId)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy Merchant"));
    }

    // DANH SÁCH MÓN

    // GET /merchant/foods
    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Merchant merchant = getCurrentMerchant();

        Pageable pageable = PageRequest.of(page, size);

        Page<FoodResponse> foodPage =
                foodService.getFoods(
                        merchant.getId(),
                        pageable
                );

        model.addAttribute("foods", foodPage.getContent());
        model.addAttribute("currentPage", foodPage.getNumber());
        model.addAttribute("totalPages", foodPage.getTotalPages());
        model.addAttribute("pageSize", foodPage.getSize());

        return "food/list";
    }

    //  FORM THÊM MÓN

    // GET /merchant/foods/create
    @GetMapping("/create")
    public String create(Model model) {

        Merchant merchant = getCurrentMerchant();

        model.addAttribute(
                "addresses",
                merchantAddressRepository.findByMerchantId(
                        merchant.getId()
                )
        );

        model.addAttribute(
                "categories",
                foodCategoryRepository.findAll()
        );

        model.addAttribute(
                "tags",
                tagRepository.findAll()
        );

        model.addAttribute(
                "coupons",
                couponRepository.findAllByMerchant_IdOrderByCreatedAtDesc(
                        merchant.getId()
                )
        );

        model.addAttribute(
                "coupons",
                couponRepository.findAllByMerchant_IdAndIsActiveTrueOrderByCreatedAtDesc(
                        merchant.getId()
                )
        );

        return "food/create";
    }

    //  CHI TIẾT MÓN
    // GET /merchant/foods/{foodId}
    @GetMapping("/{foodId}")
    public String detail(
            @PathVariable("foodId") Long foodId,
            Model model) {

        Merchant merchant = getCurrentMerchant();

        FoodResponse food =
                foodService.getFood(
                        merchant.getId(),
                        foodId
                );

        model.addAttribute("food", food);

        return "food/detail";
    }

    //  FORM CHỈNH SỬA

    // GET /merchant/foods/{foodId}/edit
    @GetMapping("/{foodId}/edit")
    public String edit(
            @PathVariable("foodId") Long foodId,
            Model model) {

        Merchant merchant = getCurrentMerchant();

        FoodResponse food =
                foodService.getFood(
                        merchant.getId(),
                        foodId
                );

        model.addAttribute("food", food);

        model.addAttribute(
                "addresses",
                merchantAddressRepository.findByMerchantId(
                        merchant.getId()
                )
        );

        model.addAttribute(
                "categories",
                foodCategoryRepository.findAll()
        );

        model.addAttribute(
                "tags",
                tagRepository.findAll()
        );

        model.addAttribute(
                "coupons",
                couponRepository.findAllByMerchant_IdOrderByCreatedAtDesc(
                        merchant.getId()
                )
        );

        return "food/edit";
    }


    // Chuyển hướng mặc định: /merchant -> /merchant/stats
    @GetMapping("/merchant")
    public String merchantIndex() {
        return "redirect:/merchant/stats";
    }

    // Hiển thị giao diện stats.html
    @GetMapping("/merchant/stats")
    public String showStatsPage() {
        return "merchant/stats";
    }
}