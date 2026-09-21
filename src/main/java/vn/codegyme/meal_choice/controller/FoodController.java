package vn.codegyme.meal_choice.controller;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.ui.Model;
import vn.codegyme.meal_choice.dto.food.FoodCreateRequest;
import vn.codegyme.meal_choice.dto.food.FoodResponse;
import vn.codegyme.meal_choice.dto.food.FoodUpdateRequest;
import vn.codegyme.meal_choice.entity.*;
import vn.codegyme.meal_choice.repository.FoodRepository;
import vn.codegyme.meal_choice.repository.MerchantRepository;
import vn.codegyme.meal_choice.repository.UserRepository;
import vn.codegyme.meal_choice.security.CustomUserDetails;
import vn.codegyme.meal_choice.service.FoodService;

import java.util.*;

@Slf4j
@Controller
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;
    private final FoodRepository foodRepository;
    private final MerchantRepository merchantRepository;
    private final UserRepository userRepository;

    // Merchant đang đăng nhập
    private Merchant getCurrentMerchant() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new RuntimeException("Người dùng chưa đăng nhập");
        }

        UUID userId = userDetails.getId();

        return merchantRepository.findByUser_Id(userId)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy Merchant"));
    }

    // MERCHANT FOOD API

    // Thêm món
    @PostMapping(
            value = "/api/merchant/foods",
            consumes = "multipart/form-data"
    )
    @ResponseBody
    public ResponseEntity<FoodResponse> createFood(
            @Valid @ModelAttribute FoodCreateRequest request,
            @RequestParam("images") List<MultipartFile> images) {

        Merchant merchant = getCurrentMerchant();

        FoodResponse response = foodService.createFood(
                merchant.getId(),
                request,
                images
        );

        return ResponseEntity.ok(response);
    }

    // Lấy danh sách món có phân trang
    @GetMapping("/api/merchant/foods")
    @ResponseBody
    public ResponseEntity<Page<FoodResponse>> getFoods(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Merchant merchant = getCurrentMerchant();

        Pageable pageable = PageRequest.of(page, size);

        return ResponseEntity.ok(
                foodService.getFoods(
                        merchant.getId(),
                        pageable
                )
        );
    }

    // Tìm kiếm món ăn của Merchant theo từ khóa
    @GetMapping("/api/merchant/foods/search")
    @ResponseBody
    public ResponseEntity<Page<FoodResponse>> searchMerchantFoods(
            @RequestParam(value = "name", required = false, defaultValue = "") String name,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        Merchant merchant = getCurrentMerchant();
        Pageable pageable = PageRequest.of(page, size);
        Page<FoodResponse> result = foodService.searchMerchantFoodsByKeyword(
                merchant.getId(),
                name,
                pageable
        );
        return ResponseEntity.ok(result);
    }

    // Chi tiết món
    @GetMapping("/api/merchant/foods/{foodId}")
    @ResponseBody
    public ResponseEntity<FoodResponse> getFood(
            @PathVariable("foodId") Long foodId) {

        Merchant merchant = getCurrentMerchant();

        return ResponseEntity.ok(
                foodService.getFood(
                        merchant.getId(),
                        foodId
                )
        );
    }

    @PutMapping(
            value = "/api/merchant/foods/{foodId}",
            consumes = "multipart/form-data"
    )
    @ResponseBody
    public ResponseEntity<FoodResponse> updateFood(
            @PathVariable("foodId") Long foodId,
            @Valid @ModelAttribute FoodUpdateRequest request,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "deletedImages", required = false) List<String> deletedImages) {

        Merchant merchant = getCurrentMerchant();

        return ResponseEntity.ok(
                foodService.updateFood(
                        merchant.getId(),
                        foodId,
                        request,
                        images,
                        deletedImages
                )
        );
    }

    // Xóa
    @DeleteMapping("/api/merchant/foods/{foodId}")
    @ResponseBody
    public ResponseEntity<String> deleteFood(
            @PathVariable("foodId") Long foodId) {

        Merchant merchant = getCurrentMerchant();

        foodService.deleteFood(
                merchant.getId(),
                foodId
        );

        return ResponseEntity.ok("Xóa món ăn thành công");
    }

    // Bật/tắt đề xuất
    @PatchMapping("/api/merchant/foods/{foodId}/recommend")
    @ResponseBody
    public ResponseEntity<Void> toggleRecommendation(
            @PathVariable("foodId") Long foodId) {

        Merchant merchant = getCurrentMerchant();

        foodService.toggleRecommendation(
                merchant.getId(),
                foodId
        );

        return ResponseEntity.ok().build();
    }

    //FOOD DETAIL PAGE
    @Transactional
    @GetMapping("/foods/{id}")
    public String foodDetailPage(
            @PathVariable("id") Long id,
            Authentication authentication,
            Model model) {

        try {
            Food food = foodRepository.findById(id).orElse(null);

            if (food == null
                    || !Boolean.TRUE.equals(food.getIsActive())
                    || food.getDeletedAt() != null) {
                return "redirect:/";
            }

            model.addAttribute("food", food);

            // Lấy danh sách món của Merchant theo thứ tự mới nhất
            List<Food> merchantFoods =
                    foodRepository
                            .findByMerchant_IdAndDeletedAtIsNullOrderByCreatedAtDesc(
                                    food.getMerchant().getId()
                            );

// Tìm vị trí món hiện tại
            int currentFoodIndex = -1;

            for (int i = 0; i < merchantFoods.size(); i++) {
                if (merchantFoods.get(i).getId().equals(food.getId())) {
                    currentFoodIndex = i;
                    break;
                }
            }

            Food previousFood = null;
            Food nextFood = null;

            if (currentFoodIndex > 0) {
                previousFood = merchantFoods.get(currentFoodIndex - 1);
            }

            if (currentFoodIndex >= 0
                    && currentFoodIndex < merchantFoods.size() - 1) {
                nextFood = merchantFoods.get(currentFoodIndex + 1);
            }

            model.addAttribute("previousFood", previousFood);
            model.addAttribute("nextFood", nextFood);

            List<String> foodImagesList = new ArrayList<>();

            if (food.getImages() != null) {
                food.getImages().stream()
                        .filter(image ->
                                image.getImageUrl() != null
                                        && !image.getImageUrl().isBlank())
                        .sorted((a, b) -> Boolean.compare(
                                !Boolean.TRUE.equals(a.getIsPrimary()),
                                !Boolean.TRUE.equals(b.getIsPrimary())
                        ))
                        .forEach(image -> {
                            if (!foodImagesList.contains(
                                    image.getImageUrl())) {
                                foodImagesList.add(
                                        image.getImageUrl()
                                );
                            }
                        });
            }

            model.addAttribute(
                    "foodImagesList",
                    foodImagesList
            );

            // Lấy danh sách thế mạnh (Top 2 danh mục có nhiều món nhất của quán)
            List<FoodCategory> storeSpecialties = Collections.emptyList();
            if (food.getMerchant() != null) {
                storeSpecialties = foodRepository.findTopCategoriesByMerchantId(
                        food.getMerchant().getId(),
                        PageRequest.of(0, 2)
                );
            }
            model.addAttribute("storeSpecialties", storeSpecialties);
            List<Food> allActiveFoods =
                    foodRepository
                            .findAllByIsActiveTrueAndDeletedAtIsNullOrderByIdDesc(
                                    org.springframework.data.domain.Pageable.unpaged()
                            )
                            .getContent();

            List<Food> recommendedFoods =
                    buildRecommendedFoods(
                            food,
                            allActiveFoods
                    );

            model.addAttribute(
                    "recommendedFoods",
                    recommendedFoods
            );

            List<Food> peopleAlsoLiked =
                    buildPeopleAlsoLiked(
                            food,
                            allActiveFoods
                    );

            model.addAttribute(
                    "peopleAlsoLiked",
                    peopleAlsoLiked
            );

            addUserInformation(
                    authentication,
                    model
            );

            return "food/customer-detail";

        } catch (Exception e) {

            log.error(
                    "Lỗi tải chi tiết món: {}",
                    e.getMessage(),
                    e
            );

            return "redirect:/";
        }
    }

    // Món đề xuất
    private List<Food> buildRecommendedFoods(
            Food food,
            List<Food> allActiveFoods) {

        List<Food> recommendedFoods =
                new ArrayList<>();

        Merchant merchant =
                food.getMerchant();

        Long currentFoodId =
                food.getId();

        List<FoodCategory> categories =
                food.getFoodCategories();

        List<Long> categoryIds =
                categories == null
                        ? Collections.emptyList()
                        : categories.stream()
                        .map(FoodCategory::getId)
                        .toList();

        List<Food> candidates =
                allActiveFoods.stream()
                        .filter(f ->
                                f != null
                                        && !f.getId()
                                        .equals(currentFoodId))
                        .toList();

        // 1. Món cùng Merchant và cùng Danh mục
        if (merchant != null) {
            List<Food> sameMerchantAndCategory =
                    candidates.stream()
                            .filter(f ->
                                    f.getMerchant() != null
                                            && f.getMerchant()
                                            .getId()
                                            .equals(merchant.getId()))
                            .filter(f ->
                                    f.getFoodCategories() != null
                                            && f.getFoodCategories()
                                            .stream()
                                            .anyMatch(category ->
                                                    categoryIds.contains(
                                                            category.getId())))
                            .sorted((a, b) ->
                                    Long.compare(b.getId(), a.getId()))
                            .limit(8)
                            .toList();

            recommendedFoods.addAll(sameMerchantAndCategory);

            // 2. Món cùng Merchant (khác danh mục)
            if (recommendedFoods.size() < 8) {
                List<Food> sameMerchantOther =
                        candidates.stream()
                                .filter(f ->
                                        !recommendedFoods.contains(f))
                                .filter(f ->
                                        f.getMerchant() != null
                                                && f.getMerchant()
                                                .getId()
                                                .equals(merchant.getId()))
                                .limit(8 - recommendedFoods.size())
                                .toList();

                recommendedFoods.addAll(sameMerchantOther);
            }
        }

        // 3. Món cùng danh mục từ các quán khác
        if (recommendedFoods.size() < 8) {
            List<Food> sameCategory =
                    candidates.stream()
                            .filter(f ->
                                    !recommendedFoods.contains(f))
                            .filter(f ->
                                    f.getFoodCategories() != null
                                            && f.getFoodCategories()
                                            .stream()
                                            .anyMatch(category ->
                                                    categoryIds.contains(
                                                            category.getId())))
                            .sorted((a, b) -> {
                                int orderA = a.getOrderCount() != null ? a.getOrderCount() : 0;
                                int orderB = b.getOrderCount() != null ? b.getOrderCount() : 0;
                                return Integer.compare(orderB, orderA);
                            })
                            .limit(8 - recommendedFoods.size())
                            .toList();

            recommendedFoods.addAll(sameCategory);
        }

        // 4. Món đề cử hoặc món bán chạy/view cao trên hệ thống
        if (recommendedFoods.size() < 8) {
            List<Food> popularFallback =
                    candidates.stream()
                            .filter(f ->
                                    !recommendedFoods.contains(f))
                            .sorted((a, b) -> {
                                boolean recA = Boolean.TRUE.equals(a.getIsRecommended());
                                boolean recB = Boolean.TRUE.equals(b.getIsRecommended());
                                if (recA != recB) return Boolean.compare(recB, recA);
                                int viewsA = a.getViews() != null ? a.getViews() : 0;
                                int viewsB = b.getViews() != null ? b.getViews() : 0;
                                return Integer.compare(viewsB, viewsA);
                            })
                            .limit(8 - recommendedFoods.size())
                            .toList();

            recommendedFoods.addAll(popularFallback);
        }

        return recommendedFoods;
    }

    // Món mọi người cùng thích
    private List<Food> buildPeopleAlsoLiked(
            Food food,
            List<Food> allActiveFoods) {

        List<Food> peopleAlsoLiked =
                new ArrayList<>();

        Long currentFoodId =
                food.getId();

        List<FoodCategory> categories =
                food.getFoodCategories();

        List<Long> categoryIds =
                categories == null
                        ? Collections.emptyList()
                        : categories.stream()
                        .map(FoodCategory::getId)
                        .toList();

        List<Food> candidates =
                allActiveFoods.stream()
                        .filter(f ->
                                f != null
                                        && !f.getId()
                                        .equals(currentFoodId))
                        .toList();

        // 1. Món cùng danh mục có lượt mua / lượt xem cao
        List<Food> sameCategoryTopOrders =
                candidates.stream()
                        .filter(f ->
                                f.getFoodCategories() != null
                                        && f.getFoodCategories()
                                        .stream()
                                        .anyMatch(category ->
                                                categoryIds.contains(
                                                        category.getId())))
                        .sorted((a, b) -> {
                            int orderA = a.getOrderCount() != null ? a.getOrderCount() : 0;
                            int orderB = b.getOrderCount() != null ? b.getOrderCount() : 0;
                            if (orderA != orderB) return Integer.compare(orderB, orderA);
                            int viewsA = a.getViews() != null ? a.getViews() : 0;
                            int viewsB = b.getViews() != null ? b.getViews() : 0;
                            return Integer.compare(viewsB, viewsA);
                        })
                        .limit(8)
                        .toList();

        peopleAlsoLiked.addAll(sameCategoryTopOrders);

        // 2. Món phổ biến nhất trên toàn hệ thống
        if (peopleAlsoLiked.size() < 8) {
            List<Food> fallback =
                    candidates.stream()
                            .filter(f ->
                                    !peopleAlsoLiked.contains(f))
                            .sorted((a, b) -> {
                                int viewsA = a.getViews() != null ? a.getViews() : 0;
                                int viewsB = b.getViews() != null ? b.getViews() : 0;
                                return Integer.compare(viewsB, viewsA);
                            })
                            .limit(8 - peopleAlsoLiked.size())
                            .toList();

            peopleAlsoLiked.addAll(fallback);
        }

        return peopleAlsoLiked;
    }

    // Thêm thông tin User
    private void addUserInformation(
            Authentication authentication,
            Model model) {

        boolean isLoggedIn = false;
        String userDisplayName = null;
        boolean isAdmin = false;
        boolean isMerchant = false;
        List<Long> likedFoodIds = Collections.emptyList();
        List<UUID> likedMerchantIds = Collections.emptyList();

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal()
                instanceof CustomUserDetails userDetails) {

            Optional<User> userOpt =
                    userRepository.findByEmail(
                            userDetails.getUsername()
                    );

            if (userOpt.isPresent()) {

                User user = userOpt.get();

                isLoggedIn = true;
                userDisplayName =
                        user.getDisplayName();

                if (user.getRoles() != null) {

                    isAdmin =
                            user.getRoles()
                                     .stream()
                                     .anyMatch(role ->
                                             role.getName() != null
                                                     && role.getName()
                                                     .name()
                                                     .contains("ADMIN"));

                    isMerchant =
                            user.getRoles()
                                     .stream()
                                     .anyMatch(role ->
                                             role.getName() != null
                                                     && role.getName()
                                                     .name()
                                                     .contains("MERCHANT"));
                }

                likedFoodIds = foodRepository.findLikedFoodIdsByUserId(user.getId().toString());
                likedMerchantIds = merchantRepository.findLikedMerchantIdsByUserId(user.getId());
            }
        }

        model.addAttribute(
                "isLoggedIn",
                isLoggedIn
        );

        model.addAttribute(
                "userDisplayName",
                userDisplayName
        );

        model.addAttribute(
                "isAdmin",
                isAdmin
        );

        model.addAttribute(
                "isMerchant",
                isMerchant
        );

        model.addAttribute(
                "likedFoodIds",
                likedFoodIds
        );

        model.addAttribute(
                "likedMerchantIds",
                likedMerchantIds
        );
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userRepository.findById(userDetails.getId()).orElse(null);
        }
        return userRepository.findByEmail(authentication.getName()).orElse(null);
    }

    @ResponseBody
    @GetMapping("/api/foods/{id}/merchant-info")
    public ResponseEntity<?> getFoodMerchantInfo(@PathVariable("id") Long id) {
        Food food = foodRepository.findById(id).orElse(null);
        if (food == null || food.getMerchant() == null) {
            return ResponseEntity.notFound().build();
        }
        Merchant m = food.getMerchant();
        return ResponseEntity.ok(Map.of(
                "merchantId", m.getId().toString(),
                "merchantRestaurantName", m.getMerchantRestaurantName() != null ? m.getMerchantRestaurantName() : ""
        ));
    }

    // LIKE FOOD

    @ResponseBody
    @PostMapping("/api/foods/{id}/like")
    public ResponseEntity<?> likeFood(
            @PathVariable("id") Long id,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Chưa đăng nhập");
        }

        foodRepository.likeFood(id, user.getId().toString());
        return ResponseEntity.ok(Map.of("success", true, "liked", true));
    }

    @ResponseBody
    @PostMapping("/api/foods/{id}/unlike")
    public ResponseEntity<?> unlikeFood(
            @PathVariable("id") Long id,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Chưa đăng nhập");
        }

        foodRepository.unlikeFood(id, user.getId().toString());
        return ResponseEntity.ok(Map.of("success", true, "liked", false));
    }

    // ==================== FOLLOW MERCHANT ====================

    @ResponseBody
    @PostMapping("/api/merchants/{id}/follow")
    public ResponseEntity<?> followMerchant(
            @PathVariable("id") UUID id,
            Authentication authentication) {

        log.info("=== FOLLOW REQUEST === merchantId={}, auth={}", id, authentication != null ? authentication.getName() : "NULL");

        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            log.warn("=== FOLLOW FAILED === User không xác thực được");
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Chưa đăng nhập");
        }

        log.info("=== FOLLOW === merchantId={}, userId={}", id, user.getId());

        int rows = merchantRepository.followMerchant(id, user.getId());
        log.info("=== FOLLOW RESULT === rows affected: {}", rows);

        long followerCount = merchantRepository.countFollowersByMerchantId(id);
        log.info("=== FOLLOW DONE === followerCount={}", followerCount);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "followed", true,
                "followerCount", followerCount
        ));
    }

    @ResponseBody
    @PostMapping("/api/merchants/{id}/unfollow")
    public ResponseEntity<?> unfollowMerchant(
            @PathVariable("id") UUID id,
            Authentication authentication) {

        log.info("=== UNFOLLOW REQUEST === merchantId={}, auth={}", id, authentication != null ? authentication.getName() : "NULL");

        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            log.warn("=== UNFOLLOW FAILED === User không xác thực được");
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Chưa đăng nhập");
        }

        log.info("=== UNFOLLOW === merchantId={}, userId={}", id, user.getId());

        int rows = merchantRepository.unfollowMerchant(id, user.getId());
        log.info("=== UNFOLLOW RESULT === rows affected: {}", rows);

        long followerCount = merchantRepository.countFollowersByMerchantId(id);
        log.info("=== UNFOLLOW DONE === followerCount={}", followerCount);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "followed", false,
                "followerCount", followerCount
        ));
    }

    @ResponseBody
    @GetMapping("/api/foods/{id}/coupons")
    public ResponseEntity<?> getFoodCoupons(@PathVariable("id") Long id) {
        Food food = foodRepository.findById(id).orElse(null);

        if (food == null) {
            return ResponseEntity.notFound().build();
        }

        List<Coupon> coupons = food.getCoupons();

        if (coupons == null || coupons.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<Map<String, Object>> result = coupons.stream()
                .map(coupon -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", coupon.getId());
                    data.put("couponCode", coupon.getCouponCode());
                    data.put("discountType", coupon.getDiscountType());
                    data.put("discountValue", coupon.getDiscountValue());
                    data.put("startAt", coupon.getStartAt());
                    return data;
                })
                .toList();

        return ResponseEntity.ok(result);
    }
}