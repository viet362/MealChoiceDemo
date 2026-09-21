package vn.codegyme.meal_choice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import vn.codegyme.meal_choice.dto.order.OrderResponseDTO;
import vn.codegyme.meal_choice.entity.Address;
import vn.codegyme.meal_choice.entity.Food;
import vn.codegyme.meal_choice.entity.FoodCategory;
import vn.codegyme.meal_choice.entity.Merchant;
import vn.codegyme.meal_choice.entity.MerchantAddress;
import vn.codegyme.meal_choice.entity.OrderStatus;
import vn.codegyme.meal_choice.entity.PaymentMethod;
import vn.codegyme.meal_choice.entity.User;
import vn.codegyme.meal_choice.repository.AddressRepository;
import vn.codegyme.meal_choice.repository.FoodCategoryRepository;
import vn.codegyme.meal_choice.repository.FoodRepository;
import vn.codegyme.meal_choice.repository.MerchantRepository;
import vn.codegyme.meal_choice.repository.UserRepository;
import vn.codegyme.meal_choice.security.CustomUserDetails;
import vn.codegyme.meal_choice.service.AuthService;
import vn.codegyme.meal_choice.service.FoodService;
import vn.codegyme.meal_choice.service.MerchantOrderService;
import vn.codegyme.meal_choice.service.UserOrderService;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PageController {

    private final MerchantRepository merchantRepository;
    private final FoodCategoryRepository foodCategoryRepository;
    private final FoodRepository foodRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final AuthService authService;
    private final FoodService foodService;
    private final MerchantOrderService merchantOrderService;
    private final UserOrderService userOrderService;

    // Lấy danh sách category
    private List<FoodCategory> getCategories() {
        List<FoodCategory> categories = foodCategoryRepository.findAll();

        if (categories == null || categories.isEmpty()) {
            return Collections.emptyList();
        }

        return categories;
    }

    // Lấy khu vực của user hiện tại
    private String getUserLocation(Authentication authentication) {
        try {
            if (authentication != null
                    && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {

                Optional<User> userOpt =
                        userRepository.findByEmail(userDetails.getUsername());

                if (userOpt.isPresent()) {
                    List<Address> addresses =
                            addressRepository.findByUserId(userOpt.get().getId());

                    if (!addresses.isEmpty()) {
                        Address targetAddress = addresses.stream()
                                .filter(address ->
                                        Boolean.TRUE.equals(address.getIsDefault()))
                                .findFirst()
                                .orElse(addresses.get(addresses.size() - 1));

                        if (targetAddress.getDistrict() != null) {
                            return targetAddress.getDistrict();
                        }

                        return targetAddress.getCity();
                    }
                }
            }
        } catch (Exception e) {
            log.warn(
                    "Không thể lấy khu vực của user: {}",
                    e.getMessage()
            );
        }

        return null;
    }

    // Thêm trạng thái đăng nhập và like vào model
    private void addLikeStatusToModel(
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
                && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {

            Optional<User> userOpt =
                    userRepository.findByEmail(userDetails.getUsername());

            if (userOpt.isPresent()) {
                User user = userOpt.get();

                isLoggedIn = true;
                userDisplayName = user.getDisplayName();

                if (user.getRoles() != null) {
                    isAdmin = user.getRoles()
                            .stream()
                            .anyMatch(role ->
                                    role.getName() != null
                                            && role.getName().name().contains("ADMIN"));

                    isMerchant = user.getRoles()
                            .stream()
                            .anyMatch(role ->
                                    role.getName() != null
                                            && role.getName().name().contains("MERCHANT"));
                }

                UUID userId = user.getId();

                likedFoodIds =
                        foodRepository.findLikedFoodIdsByUserId(userId.toString());

                likedMerchantIds =
                        merchantRepository.findLikedMerchantIdsByUserId(userId);
            }
        }

        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("userDisplayName", userDisplayName);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isMerchant", isMerchant);
        model.addAttribute("likedFoodIds", likedFoodIds);
        model.addAttribute("likedMerchantIds", likedMerchantIds);
    }

    // Trang chủ
    @GetMapping({"/", "/home"})
    public String homePage(
            @RequestParam(name = "page", defaultValue = "0") int page,
            Authentication authentication,
            Model model) {

        try {
            addLikeStatusToModel(authentication, model);

            Pageable pageable = PageRequest.of(Math.max(0, page), 30);

            Page<Food> foodPage =
                    foodRepository
                            .findAllByIsActiveTrueAndDeletedAtIsNullOrderByIdDesc(
                                    pageable
                            );

            model.addAttribute("foodPage", foodPage);
            model.addAttribute("categories", getCategories());

            if (page == 0) {
                Page<Food> activeFoodsPage =
                        foodRepository
                                .findAllByIsActiveTrueAndDeletedAtIsNullOrderByIdDesc(
                                        PageRequest.of(0, 100)
                                );

                List<Food> allActive = activeFoodsPage != null
                        ? activeFoodsPage.getContent()
                        : Collections.emptyList();

                // Lấy 8 món giảm giá nhiều nhất
                List<Food> topDiscountFoods = allActive.stream()
                        .filter(food ->
                                food != null
                                        && food.getPrice() != null
                                        && food.getDiscountPrice() != null
                                        && food.getPrice()
                                        .compareTo(BigDecimal.ZERO) > 0
                                        && food.getDiscountPrice()
                                        .compareTo(food.getPrice()) < 0
                        )
                        .sorted((food1, food2) -> {
                            BigDecimal discount1 =
                                    food1.getPrice()
                                            .subtract(food1.getDiscountPrice())
                                            .divide(
                                                    food1.getPrice(),
                                                    4,
                                                    java.math.RoundingMode.HALF_UP
                                            );

                            BigDecimal discount2 =
                                    food2.getPrice()
                                            .subtract(food2.getDiscountPrice())
                                            .divide(
                                                    food2.getPrice(),
                                                    4,
                                                    java.math.RoundingMode.HALF_UP
                                            );

                            return discount2.compareTo(discount1);
                        })
                        .limit(8)
                        .toList();

                if (topDiscountFoods.size() < 8 && !allActive.isEmpty()) {
                    topDiscountFoods = allActive.stream()
                            .limit(8)
                            .toList();
                }

                model.addAttribute("topDiscountFoods", topDiscountFoods);

                // Lấy 8 món gần khu vực user
                List<Food> suggestedFoods = new ArrayList<>();

                String userLocation = getUserLocation(authentication);

                if (userLocation != null && !userLocation.isBlank()) {
                    String cleanLocation = userLocation.toLowerCase();

                    suggestedFoods = allActive.stream()
                            .filter(food ->
                                    food != null
                                            && food.getMerchantAddress() != null
                                            && food.getMerchantAddress()
                                            .getMerchantAddress() != null
                                            && food.getMerchantAddress()
                                            .getMerchantAddress()
                                            .toLowerCase()
                                            .contains(cleanLocation)
                            )
                            .limit(8)
                            .toList();
                }

                if (suggestedFoods.size() < 8 && !allActive.isEmpty()) {
                    suggestedFoods = allActive.stream()
                            .limit(8)
                            .toList();
                }

                model.addAttribute("suggestedFoods", suggestedFoods);

            } else {
                model.addAttribute(
                        "topDiscountFoods",
                        Collections.emptyList()
                );
                model.addAttribute(
                        "suggestedFoods",
                        Collections.emptyList()
                );
            }

        } catch (Exception e) {
            log.error(
                    "Lỗi khi load trang chủ: {}",
                    e.getMessage(),
                    e
            );

            model.addAttribute("categories", Collections.emptyList());
            model.addAttribute("topDiscountFoods", Collections.emptyList());
            model.addAttribute("suggestedFoods", Collections.emptyList());
            model.addAttribute("foodPage", Page.empty());
        }

        return "home";
    }

    // Trang tìm kiếm và lọc theo category
    @GetMapping("/search")
    public String searchPage(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Authentication authentication,
            Model model) {

        addLikeStatusToModel(authentication, model);

        Pageable pageable = PageRequest.of(Math.max(0, page), 50);
        String cleanKeyword = keyword != null && !keyword.trim().isEmpty()
                ? keyword.trim()
                : null;

        Page<Food> foodPage;

        try {
            if (cleanKeyword != null && categoryId != null) {
                foodPage = foodRepository.searchByKeywordAndCategory(
                        cleanKeyword, categoryId, pageable);
            } else if (cleanKeyword != null) {
                foodPage = foodRepository.searchByKeyword(
                        cleanKeyword, pageable);
            } else if (categoryId != null) {
                foodPage = foodRepository.findAllByCategoryId(
                        categoryId, pageable);
            } else {
                foodPage = foodRepository
                        .findAllByIsActiveTrueAndDeletedAtIsNullOrderByIdDesc(pageable);
            }
        } catch (Exception e) {
            log.error("Lỗi tìm kiếm: {}", e.getMessage(), e);
            foodPage = Page.empty();
        }

        model.addAttribute("foodPage", foodPage);
        model.addAttribute("categories", getCategories());
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("keyword", cleanKeyword);
        model.addAttribute("currentUrl", "/search");
        model.addAttribute("pageTitle", "Kết Quả Tìm Kiếm");
        model.addAttribute(
                "pageDescription",
                cleanKeyword != null
                        ? "Kết quả tìm kiếm cho: \"" + cleanKeyword + "\""
                        : "Tất cả món ăn"
        );

        return "homepage/food-list-page";
    }

    // Trang món ăn gần bạn
    @GetMapping("/restaurants/near-you")
    public String nearYouPage(
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Authentication authentication,
            Model model) {

        addLikeStatusToModel(authentication, model);

        Pageable pageable = PageRequest.of(Math.max(0, page), 50);
        String userLocation = getUserLocation(authentication);

        Page<Food> foodPage;

        try {
            if (userLocation != null && !userLocation.isBlank()) {
                foodPage = categoryId != null
                        ? foodRepository.findByLocationNearUserAndCategory(
                        userLocation, categoryId, pageable)
                        : foodRepository.findByLocationNearUser(
                        userLocation, pageable);

                if (foodPage.isEmpty()) {
                    foodPage = categoryId != null
                            ? foodRepository.findAllByCategoryId(
                            categoryId, pageable)
                            : foodRepository
                            .findAllByIsActiveTrueAndDeletedAtIsNullOrderByIdDesc(
                                    pageable);
                }
            } else {
                foodPage = categoryId != null
                        ? foodRepository.findAllByCategoryId(categoryId, pageable)
                        : foodRepository
                        .findAllByIsActiveTrueAndDeletedAtIsNullOrderByIdDesc(
                                pageable);
            }
        } catch (Exception e) {
            log.error("Lỗi trang gần bạn: {}", e.getMessage(), e);
            foodPage = Page.empty();
        }

        model.addAttribute("foodPage", foodPage);
        model.addAttribute("categories", getCategories());
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("currentUrl", "/restaurants/near-you");
        model.addAttribute("pageTitle", "Món Ngon Gần Bạn");
        model.addAttribute(
                "pageDescription",
                userLocation != null
                        ? "Các quán ăn gần khu vực " + userLocation
                        : "Giao hàng từ các quán ăn gần bạn"
        );

        return "homepage/food-list-page";
    }

    // Trang món ăn nổi bật
    @GetMapping({
            "/restaurants/popular",
            "/restaurants/top-picks"
    })
    public String popularPage(
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Authentication authentication,
            Model model) {

        addLikeStatusToModel(authentication, model);

        Pageable pageable = PageRequest.of(Math.max(0, page), 50);
        Page<Food> foodPage;

        try {
            if (categoryId != null) {
                foodPage = foodRepository.findTopDiscountsByCategory(
                        categoryId, pageable);

                if (foodPage.isEmpty()) {
                    foodPage = foodRepository.findAllByCategoryId(
                            categoryId, pageable);
                }
            } else {
                foodPage = foodRepository.findAllTopDiscounts(pageable);

                if (foodPage.isEmpty()) {
                    foodPage = foodRepository
                            .findAllByIsActiveTrueAndDeletedAtIsNullOrderByIdDesc(
                                    pageable);
                }
            }
        } catch (Exception e) {
            log.error("Lỗi trang nổi bật: {}", e.getMessage(), e);
            foodPage = Page.empty();
        }

        model.addAttribute("foodPage", foodPage);
        model.addAttribute("categories", getCategories());
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("currentUrl", "/restaurants/popular");
        model.addAttribute("pageTitle", "Món Ăn Nổi Bật & Bán Chạy");
        model.addAttribute(
                "pageDescription",
                "Những món ăn được đánh giá cao và đặt nhiều"
        );

        return "homepage/food-list-page";
    }

    // Trang món ăn giảm giá
    @GetMapping("/restaurants/top-discounts")
    public String topDiscountsPage(
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Authentication authentication,
            Model model) {

        addLikeStatusToModel(authentication, model);

        Pageable pageable = PageRequest.of(Math.max(0, page), 50);
        Page<Food> foodPage;

        try {
            foodPage = categoryId != null
                    ? foodRepository.findTopDiscountsByCategory(
                    categoryId, pageable)
                    : foodRepository.findAllTopDiscounts(pageable);
        } catch (Exception e) {
            log.error("Lỗi trang ưu đãi: {}", e.getMessage(), e);
            foodPage = Page.empty();
        }

        model.addAttribute("foodPage", foodPage);
        model.addAttribute("categories", getCategories());
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("currentUrl", "/restaurants/top-discounts");
        model.addAttribute("pageTitle", "Siêu Ưu Đãi");
        model.addAttribute(
                "pageDescription",
                "Các món ăn đang có ưu đãi giảm giá"
        );

        return "homepage/food-list-page";
    }

    // Trang nhà hàng mới
    @GetMapping("/restaurants/new")
    public String newRestaurantsPage(
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Authentication authentication,
            Model model) {

        addLikeStatusToModel(authentication, model);

        Pageable pageable = PageRequest.of(Math.max(0, page), 50);
        Page<Food> foodPage;

        try {
            foodPage = categoryId != null
                    ? foodRepository.findAllByCategoryId(categoryId, pageable)
                    : foodRepository
                    .findAllByIsActiveTrueAndDeletedAtIsNullOrderByIdDesc(
                            pageable);
        } catch (Exception e) {
            log.error("Lỗi trang nhà hàng mới: {}", e.getMessage(), e);
            foodPage = Page.empty();
        }

        model.addAttribute("foodPage", foodPage);
        model.addAttribute("categories", getCategories());
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("currentUrl", "/restaurants/new");
        model.addAttribute("pageTitle", "Quán Ăn & Nhà Hàng Mới");
        model.addAttribute(
                "pageDescription",
                "Khám phá những quán ăn mới"
        );

        return "homepage/food-list-page";
    }

    // Trang đối tác chọn lọc
    @GetMapping("/restaurants/trusted")
    public String trustedRestaurantsPage(
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Authentication authentication,
            Model model) {

        addLikeStatusToModel(authentication, model);

        Pageable pageable = PageRequest.of(Math.max(0, page), 50);
        Page<Food> foodPage;

        try {
            foodPage = categoryId != null
                    ? foodRepository.findTrustedFoodsByCategory(categoryId, pageable)
                    : foodRepository.findTrustedFoods(pageable);

            if (foodPage.isEmpty()) {
                foodPage = categoryId != null
                        ? foodRepository.findAllByCategoryId(categoryId, pageable)
                        : foodRepository.findAllByIsActiveTrueAndDeletedAtIsNullOrderByIdDesc(pageable);
            }
        } catch (Exception e) {
            log.error("Lỗi trang đối tác chọn lọc: {}", e.getMessage(), e);
            foodPage = Page.empty();
        }

        model.addAttribute("foodPage", foodPage);
        model.addAttribute("categories", getCategories());
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("currentUrl", "/restaurants/trusted");
        model.addAttribute("pageTitle", "Đối Tác Chọn Lọc");
        model.addAttribute(
                "pageDescription",
                "Thương hiệu uy tín, chất lượng đảm bảo và được đánh giá cao"
        );

        return "homepage/food-list-page";
    }

    // Trang đăng nhập
    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    // Trang kích hoạt tài khoản
    @GetMapping("/activate")
    public String activatePage(
            @RequestParam("token") String token,
            Model model) {

        try {
            authService.activateAccount(token);

            model.addAttribute("success", true);
            model.addAttribute(
                    "message",
                    "Tài khoản của bạn đã được kích hoạt thành công!"
            );

        } catch (RuntimeException ex) {
            model.addAttribute("success", false);
            model.addAttribute("message", ex.getMessage());
        }

        return "auth/activate";
    }

    // Trang hồ sơ user
    @GetMapping("/user/profile")
    public String profilePage() {
        return "user/profile";
    }

    // Trang địa chỉ user
    @GetMapping("/user/address")
    public String addressPage() {
        return "redirect:/user/profile";
    }

    // Trang đăng ký Merchant
    @GetMapping({
            "/merchants/register",
            "/merchant/register"
    })
    public String merchantRegisterPage() {
        return "merchant/register";
    }

    // Trang dashboard Merchant
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    @GetMapping("/merchant/dashboard")
    public String merchantDashboardPage(
            Authentication authentication,
            Model model) {

        if (authentication != null
                && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {

            String email = userDetails.getUsername();
            Optional<Merchant> merchantOpt =
                    merchantRepository.findByUser_Id(userDetails.getId());

            if (merchantOpt.isPresent()) {
                Merchant merchant = merchantOpt.get();
                model.addAttribute("merchant", merchant);
                model.addAttribute(
                        "foodCount",
                        foodService.getFoods(
                                merchant.getId(),
                                org.springframework.data.domain.PageRequest.of(0, 1)
                        ).getTotalElements()
                );
                return "merchant/dashboard";
            }
        }

        // Nếu tài khoản chưa phải là Merchant -> Chuyển hướng đến trang đăng ký
        return "redirect:/merchant/register";
    }

    // Trang hồ sơ Merchant
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    @GetMapping({
            "/merchants/profile",
            "/merchant/profile"
    })
    public String merchantProfilePage(
            Authentication authentication,
            Model model) {

        if (authentication != null
                && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {

            Optional<Merchant> merchantOpt =
                    merchantRepository.findByUser_Id(userDetails.getId());

            if (merchantOpt.isPresent()) {
                model.addAttribute("merchant", merchantOpt.get());
                return "merchant/profile";
            }
        }

        // Nếu tài khoản chưa phải là Merchant -> Chuyển hướng đến trang đăng ký
        return "redirect:/merchant/register";
    }

    // ==================== CHECKOUT & USER ORDERS ====================

    // Trang thanh toán đơn hàng (Checkout)
    @GetMapping("/checkout")
    public String checkoutPage(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return "redirect:/login?redirect=/checkout";
        }

        User user = userRepository.findById(userDetails.getId()).orElse(null);
        if (user == null) {
            return "redirect:/login?redirect=/checkout";
        }

        List<Address> addresses = addressRepository.findByUserId(user.getId());
        model.addAttribute("user", user);
        model.addAttribute("addresses", addresses);
        model.addAttribute("paymentMethods", PaymentMethod.values());

        return "checkout/checkout";
    }

    // Trang đặt hàng thành công
    @GetMapping("/checkout/success")
    public String checkoutSuccess(
            @RequestParam(name = "orderCode", required = false) String orderCode,
            @RequestParam(name = "orders", required = false) String ordersParam,
            Model model) {

        List<OrderResponseDTO> ordersList = new ArrayList<>();

        if (ordersParam != null && !ordersParam.isBlank()) {
            String[] codes = ordersParam.split(",");
            for (String code : codes) {
                try {
                    OrderResponseDTO o = userOrderService.getOrderDetailByCode(code.trim());
                    ordersList.add(o);
                } catch (Exception e) {
                    log.warn("Không tìm thấy đơn hàng mã {}: {}", code, e.getMessage());
                }
            }
        } else if (orderCode != null && !orderCode.isBlank()) {
            try {
                OrderResponseDTO order = userOrderService.getOrderDetailByCode(orderCode);
                ordersList.add(order);
                model.addAttribute("order", order); // Backward compat
            } catch (Exception e) {
                log.warn("Không tìm thấy đơn hàng mã {}: {}", orderCode, e.getMessage());
            }
        }

        model.addAttribute("ordersList", ordersList);

        return "checkout/success";
    }

    // Trang lịch sử đơn hàng của User
    @GetMapping("/user/orders")
    public String userOrdersPage(
            @RequestParam(name = "status", required = false) OrderStatus status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Authentication authentication,
            Model model) {

        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return "redirect:/login";
        }

        User user = userRepository.findById(userDetails.getId()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        int pageSize = 20;

        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                pageSize,
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "id"));

        // Truyền thêm status để lọc theo trạng thái đơn hàng
        Page<OrderResponseDTO> orderPage =
                userOrderService.getUserOrders(user.getId(), status, pageable);

        model.addAttribute("user", user);
        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("orderPage", orderPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("selectedStatus", status);

        return "user/orders";
    }

    // Trang giỏ hàng full-page
    @GetMapping("/cart")
    public String cartPage(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return "redirect:/login?redirect=/cart";
        }

        return "user/cart";
    }

    // ==================== MERCHANT ORDERS & STATS ====================

    // Chuyển hướng /merchant sang /merchant/dashboard
    @GetMapping("/merchant")
    public String merchantRootPage() {
        return "redirect:/merchant/dashboard";
    }

    // Trang thống kê của Merchant
    @GetMapping("/merchant/stats")
    public String merchantStatsPage(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return "redirect:/login";
        }

        Merchant merchant = merchantRepository.findByUser_Id(userDetails.getId()).orElse(null);
        if (merchant == null) {
            return "redirect:/merchant/register";
        }

        model.addAttribute("merchant", merchant);
        return "merchant/stats";
    }

    // Trang danh sách đơn hàng của Merchant
    @GetMapping("/merchant/orders")
    public String merchantOrdersPage(
            @RequestParam(name = "status", required = false) OrderStatus status,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Authentication authentication,
            Model model) {

        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return "redirect:/login";
        }

        Merchant merchant = merchantRepository.findByUser_Id(userDetails.getId()).orElse(null);
        if (merchant == null) {
            return "redirect:/merchant/register";
        }

        String cleanKeyword = keyword != null && !keyword.trim().isEmpty()
                ? keyword.trim()
                : null;

        int currentPage = Math.max(0, page);
        int pageSize = 50;

        Pageable pageable = PageRequest.of(
                currentPage,
                pageSize,
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC,
                        "id"
                )
        );

        Page<OrderResponseDTO> orderPage =
                merchantOrderService.getMerchantOrders(
                        merchant.getId(),
                        status,
                        cleanKeyword,
                        pageable
                );

        long pendingCount = merchantOrderService.countPendingOrders(merchant.getId());

        model.addAttribute("merchant", merchant);
        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("orderPage", orderPage);
        model.addAttribute("totalPages", orderPage.getTotalPages());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("keyword", cleanKeyword);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("orderStatuses", OrderStatus.values());

        return "merchant/orders/list";
    }

    // Trang chi tiết đơn hàng của Merchant
    @GetMapping("/merchant/orders/{id}")
    public String merchantOrderDetailPage(
            @PathVariable("id") Long id,
            Authentication authentication,
            Model model) {

        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return "redirect:/login";
        }

        Merchant merchant = merchantRepository.findByUser_Id(userDetails.getId()).orElse(null);
        if (merchant == null) {
            return "redirect:/merchant/register";
        }

        OrderResponseDTO order = merchantOrderService.getMerchantOrderDetail(merchant.getId(), id);
        model.addAttribute("merchant", merchant);
        model.addAttribute("order", order);

        return "merchant/orders/detail";
    }

    // ==================== CUSTOMER STORE PAGE ====================

    // Trang chi tiết Cửa Hàng (Customer Store Page)
    @GetMapping("/stores/{id}")
    public String storeDetailPage(
            @PathVariable("id") UUID id,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Authentication authentication,
            Model model) {

        try {
            Merchant merchant = merchantRepository.findByIdWithAddresses(id).orElse(null);
            if (merchant == null) {
                return "redirect:/";
            }

            addLikeStatusToModel(authentication, model);

            // Địa chỉ mặc định hoặc đầu tiên của quán
            MerchantAddress defaultAddress = null;
            if (merchant.getAddresses() != null && !merchant.getAddresses().isEmpty()) {
                defaultAddress = merchant.getAddresses().stream()
                        .filter(a -> a != null && a.isDefault())
                        .findFirst()
                        .orElse(merchant.getAddresses().get(0));
            }

            // Kiểm tra trạng thái mở/đóng cửa
            boolean isOpenNow = true;
            LocalTime now = LocalTime.now();
            if (defaultAddress != null && defaultAddress.getMerchantOpenTime() != null && defaultAddress.getMerchantCloseTime() != null) {
                LocalTime openTime = defaultAddress.getMerchantOpenTime();
                LocalTime closeTime = defaultAddress.getMerchantCloseTime();
                if (closeTime.isAfter(openTime)) {
                    isOpenNow = !now.isBefore(openTime) && !now.isAfter(closeTime);
                } else {
                    // Mở qua đêm
                    isOpenNow = !now.isBefore(openTime) || !now.isAfter(closeTime);
                }
            }

            // Danh sách danh mục món của riêng quán (cho các Tab thực đơn)
            List<FoodCategory> storeCategories = foodRepository.findDistinctCategoriesByMerchantId(id);

            // Top 2 danh mục có nhiều món nhất của quán (cho tag Thế mạnh quán ở Header)
            List<FoodCategory> topSpecialties = foodRepository.findTopCategoriesByMerchantId(id, PageRequest.of(0, 2));

            // Lấy danh sách món ăn (hỗ trợ tìm kiếm & lọc category)
            Pageable pageable = PageRequest.of(Math.max(0, page), 30);
            String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

            Page<Food> foodPage;
            if (cleanKeyword != null) {
                foodPage = foodRepository.searchCustomerFoodsInMerchant(id, cleanKeyword, categoryId, pageable);
            } else if (categoryId != null) {
                foodPage = foodRepository.findActiveFoodsByMerchantAndCategory(id, categoryId, pageable);
            } else {
                foodPage = foodRepository.findAllByMerchant_IdAndIsActiveTrueAndDeletedAtIsNullOrderByIdDesc(id, pageable);
            }

            // Tổng số món & Lượt theo dõi
            long totalFoods = foodRepository.countByMerchant_IdAndIsActiveTrueAndDeletedAtIsNull(id);
            long followerCount = merchantRepository.countFollowersByMerchantId(id);

            model.addAttribute("merchant", merchant);
            model.addAttribute("defaultAddress", defaultAddress);
            model.addAttribute("isOpenNow", isOpenNow);
            model.addAttribute("storeCategories", storeCategories);
            model.addAttribute("topSpecialties", topSpecialties);
            model.addAttribute("foodPage", foodPage);
            model.addAttribute("selectedCategoryId", categoryId);
            model.addAttribute("keyword", cleanKeyword);
            model.addAttribute("totalFoods", totalFoods);
            model.addAttribute("followerCount", followerCount);

            return "merchant/store/detail";

        } catch (Exception e) {
            log.error("Lỗi khi tải trang cửa hàng: {}", e.getMessage(), e);
            return "redirect:/";
        }
    }
}