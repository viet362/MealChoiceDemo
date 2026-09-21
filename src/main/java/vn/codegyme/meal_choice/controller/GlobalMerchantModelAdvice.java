package vn.codegyme.meal_choice.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.codegyme.meal_choice.entity.Merchant;
import vn.codegyme.meal_choice.entity.MerchantStatus;
import vn.codegyme.meal_choice.entity.User;
import vn.codegyme.meal_choice.repository.FoodRepository;
import vn.codegyme.meal_choice.repository.MerchantRepository;
import vn.codegyme.meal_choice.repository.UserRepository;
import vn.codegyme.meal_choice.security.CustomUserDetails;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalMerchantModelAdvice {

    private static final String CACHED_MERCHANT_KEY = "CACHED_GLOBAL_MERCHANT";
    private static final String CACHED_USER_KEY = "CACHED_GLOBAL_USER";
    private final MerchantRepository merchantRepository;
    private final UserRepository userRepository;
    private final FoodRepository foodRepository;

    @ModelAttribute
    public void addGlobalUserAndNavAttributes(Authentication authentication, HttpServletRequest request, Model model) {
        boolean isLoggedIn = false;
        String userDisplayName = null;
        String userAvatarUrl = null;
        boolean isAdmin = false;
        boolean isMerchant = false;
        boolean isMerchantBlocked = false;
        String merchantLockReason = null;

        List<Long> likedFoodIds = Collections.emptyList();
        List<UUID> likedMerchantIds = Collections.emptyList();

        User user = getCachedUser(authentication, request);
        if (user != null) {
            isLoggedIn = true;
            userDisplayName = user.getDisplayName();
            userAvatarUrl = user.getAvatarUrl();

            if (user.getRoles() != null) {
                isAdmin = user.getRoles().stream()
                        .anyMatch(role -> role.getName() != null && role.getName().name().contains("ADMIN"));
                isMerchant = user.getRoles().stream()
                        .anyMatch(role -> role.getName() != null && role.getName().name().contains("MERCHANT"));
            }

            try {
                likedFoodIds = foodRepository.findLikedFoodIdsByUserId(user.getId().toString());
                likedMerchantIds = merchantRepository.findLikedMerchantIdsByUserId(user.getId());
            } catch (Exception ignored) {
            }

            Merchant merchant = getCachedMerchant(authentication, request);
            if (merchant != null && merchant.getMerchantStatus() == MerchantStatus.BLOCKED) {
                isMerchantBlocked = true;
                merchantLockReason = merchant.getLockReason();
            }
        }

        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("userDisplayName", userDisplayName);
        model.addAttribute("currentUserName", userDisplayName);
        model.addAttribute("currentUserAvatar", userAvatarUrl);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isMerchant", isMerchant);
        model.addAttribute("isMerchantBlocked", isMerchantBlocked);
        model.addAttribute("currentMerchantBlocked", isMerchantBlocked);
        model.addAttribute("merchantLockReason", merchantLockReason);
        model.addAttribute("likedFoodIds", likedFoodIds);
        model.addAttribute("likedMerchantIds", likedMerchantIds);
    }

    private User getCachedUser(Authentication authentication, HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        if (request != null && request.getAttribute(CACHED_USER_KEY) != null) {
            Object cached = request.getAttribute(CACHED_USER_KEY);
            return cached instanceof User u ? u : null;
        }

        User user = null;
        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            user = userRepository.findById(userDetails.getId()).orElse(null);
        } else {
            String email = authentication.getName();
            user = userRepository.findByEmail(email).orElse(null);
        }

        if (request != null) {
            request.setAttribute(CACHED_USER_KEY, user != null ? user : new Object());
        }

        return user;
    }

    private Merchant getCachedMerchant(Authentication authentication, HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        if (request != null && request.getAttribute(CACHED_MERCHANT_KEY) != null) {
            Object cached = request.getAttribute(CACHED_MERCHANT_KEY);
            return cached instanceof Merchant m ? m : null;
        }

        User user = getCachedUser(authentication, request);
        Merchant merchant = null;
        if (user != null) {
            merchant = merchantRepository.findByUser_Id(user.getId()).orElse(null);
        }

        if (request != null) {
            request.setAttribute(CACHED_MERCHANT_KEY, merchant != null ? merchant : new Object());
        }

        return merchant;
    }
}
