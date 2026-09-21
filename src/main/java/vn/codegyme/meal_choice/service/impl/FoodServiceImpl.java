package vn.codegyme.meal_choice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.codegyme.meal_choice.dto.food.FoodCreateRequest;
import vn.codegyme.meal_choice.dto.food.FoodResponse;
import vn.codegyme.meal_choice.dto.food.FoodUpdateRequest;
import vn.codegyme.meal_choice.entity.Coupon;
import vn.codegyme.meal_choice.entity.Food;
import vn.codegyme.meal_choice.entity.FoodCategory;
import vn.codegyme.meal_choice.entity.FoodImage;
import vn.codegyme.meal_choice.entity.Merchant;
import vn.codegyme.meal_choice.entity.MerchantAddress;
import vn.codegyme.meal_choice.entity.MerchantStatus;
import vn.codegyme.meal_choice.entity.Tag;
import vn.codegyme.meal_choice.repository.CouponRepository;
import vn.codegyme.meal_choice.repository.FoodCategoryRepository;
import vn.codegyme.meal_choice.repository.FoodRepository;
import vn.codegyme.meal_choice.repository.MerchantAddressRepository;
import vn.codegyme.meal_choice.repository.MerchantRepository;
import vn.codegyme.meal_choice.repository.TagRepository;
import vn.codegyme.meal_choice.service.FileStorageService;
import vn.codegyme.meal_choice.service.FoodService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FoodServiceImpl implements FoodService {

    private final FoodRepository foodRepository;
    private final FoodCategoryRepository foodCategoryRepository;
    private final TagRepository tagRepository;
    private final CouponRepository couponRepository;
    private final MerchantRepository merchantRepository;
    private final MerchantAddressRepository merchantAddressRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public FoodResponse createFood(
            UUID merchantId,
            FoodCreateRequest request,
            List<MultipartFile> images) {

        Merchant merchant = getApprovedMerchant(merchantId);

        if (images == null || images.size() < 2) {
            throw new RuntimeException("Món ăn phải có ít nhất 2 ảnh");
        }

        MerchantAddress address = merchantAddressRepository
                .findById(request.getMerchantAddressId())
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy địa chỉ Merchant"));

        if (!address.getMerchant().getId().equals(merchantId)) {
            throw new RuntimeException("Địa chỉ không thuộc Merchant này");
        }

        List<FoodCategory> categories =
                foodCategoryRepository.findAllById(request.getCategoryIds());

        if (categories.size() != request.getCategoryIds().size()) {
            throw new RuntimeException("Một hoặc nhiều danh mục không tồn tại");
        }

        List<Tag> tags = Collections.emptyList();

        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            tags = tagRepository.findAllById(request.getTagIds());

            if (tags.size() != request.getTagIds().size()) {
                throw new RuntimeException("Một hoặc nhiều Tag không tồn tại");
            }
        }

        List<Coupon> coupons = Collections.emptyList();

        if (request.getCouponIds() != null && !request.getCouponIds().isEmpty()) {
            coupons = couponRepository.findAllByIdInAndMerchant_Id(
                    request.getCouponIds(),
                    merchantId
            );

            if (coupons.size() != request.getCouponIds().size()) {
                throw new RuntimeException(
                        "Một hoặc nhiều Coupon không tồn tại hoặc không thuộc Merchant này"
                );
            }
        }

        BigDecimal discountPrice = calculateDiscountPrice(
                request.getPrice(),
                request.getDiscountType(),
                request.getDiscountValue()
        );

        Food food = Food.builder()
                .merchant(merchant)
                .merchantAddress(address)
                .foodCategories(new ArrayList<>(categories))
                .tags(new ArrayList<>(tags))
                .coupons(new ArrayList<>(coupons))
                .foodName(request.getFoodName())
                .preparationTime(request.getPreparationTime())
                .foodNote(request.getFoodNote())
                .price(request.getPrice())
                .discountPrice(discountPrice)
                .serviceFee(
                        request.getServiceFee() != null
                                ? request.getServiceFee()
                                : BigDecimal.ZERO
                )
                .isActive(true)
                .isRecommended(false)
                .views(0)
                .orderCount(0)
                .build();

        food = foodRepository.save(food);

        if (food.getImages() == null) {
            food.setImages(new ArrayList<>());
        }

        for (int i = 0; i < images.size(); i++) {
            MultipartFile image = images.get(i);

            if (image == null || image.isEmpty()) {
                continue;
            }

            String imageUrl = fileStorageService.saveFoodImage(
                    food.getId(),
                    image
            );

            FoodImage foodImage = FoodImage.builder()
                    .food(food)
                    .imageUrl(imageUrl)
                    .isPrimary(i == 0)
                    .build();

            food.getImages().add(foodImage);
        }

        if (food.getImages().isEmpty()) {
            throw new RuntimeException("Món ăn phải có ít nhất 1 ảnh hợp lệ");
        }

        foodRepository.save(food);

        return mapToResponse(food);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FoodResponse> getFoods(UUID merchantId, Pageable pageable) {
        if (!merchantRepository.existsById(merchantId)) {
            throw new RuntimeException("Không tìm thấy Merchant");
        }

        return foodRepository
                .findByMerchant_IdAndDeletedAtIsNullOrderByCreatedAtDesc(
                        merchantId,
                        pageable
                )
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public FoodResponse getFood(UUID merchantId, Long foodId) {
        Food food = foodRepository
                .findByIdAndMerchant_IdAndDeletedAtIsNull(
                        foodId,
                        merchantId
                )
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy món ăn"));

        return mapToResponse(food);
    }

    @Override
    @Transactional
    public FoodResponse updateFood(
            UUID merchantId,
            Long foodId,
            FoodUpdateRequest request,
            List<MultipartFile> images,
            List<String> deletedImages) {

        Merchant merchant = getApprovedMerchant(merchantId);

        Food food = foodRepository
                .findByIdAndMerchant_IdAndDeletedAtIsNull(
                        foodId,
                        merchantId
                )
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy món ăn"));

        MerchantAddress address = merchantAddressRepository
                .findById(request.getMerchantAddressId())
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy địa chỉ Merchant"));

        if (!address.getMerchant().getId().equals(merchantId)) {
            throw new RuntimeException("Địa chỉ không thuộc Merchant này");
        }

        List<FoodCategory> categories =
                foodCategoryRepository.findAllById(request.getCategoryIds());

        if (categories.size() != request.getCategoryIds().size()) {
            throw new RuntimeException("Một hoặc nhiều danh mục không tồn tại");
        }

        List<Tag> tags = Collections.emptyList();

        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            tags = tagRepository.findAllById(request.getTagIds());

            if (tags.size() != request.getTagIds().size()) {
                throw new RuntimeException("Một hoặc nhiều Tag không tồn tại");
            }
        }

        List<Coupon> coupons = Collections.emptyList();

        if (request.getCouponIds() != null && !request.getCouponIds().isEmpty()) {
            coupons = couponRepository.findAllByIdInAndMerchant_Id(
                    request.getCouponIds(),
                    merchantId
            );

            if (coupons.size() != request.getCouponIds().size()) {
                throw new RuntimeException(
                        "Một hoặc nhiều Coupon không tồn tại hoặc không thuộc Merchant này"
                );
            }
        }

        BigDecimal discountPrice = null;

        if ("NONE".equals(request.getDiscountType())) {
            discountPrice = request.getPrice();
        } else if ("FIXED_PRICE".equals(request.getDiscountType())) {
            discountPrice = request.getDiscountPrice();

            if (discountPrice == null
                    || discountPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Giá sau khuyến mãi phải lớn hơn 0");
            }

            if (discountPrice.compareTo(request.getPrice()) >= 0) {
                throw new RuntimeException(
                        "Giá sau khuyến mãi phải nhỏ hơn giá bán gốc"
                );
            }
        } else if ("PERCENT".equals(request.getDiscountType())) {
            if (request.getDiscountValue() == null
                    || request.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0
                    || request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) >= 0) {
                throw new RuntimeException(
                        "Phần trăm giảm phải lớn hơn 0% và nhỏ hơn 100%"
                );
            }

            discountPrice = request.getPrice()
                    .multiply(
                            BigDecimal.valueOf(100)
                                    .subtract(request.getDiscountValue())
                    )
                    .divide(
                            BigDecimal.valueOf(100),
                            2,
                            RoundingMode.HALF_UP
                    );
        } else {
            throw new RuntimeException("Hình thức giảm giá không hợp lệ");
        }

        food.setMerchant(merchant);
        food.setMerchantAddress(address);
        food.setFoodCategories(new ArrayList<>(categories));
        food.setTags(new ArrayList<>(tags));
        food.setCoupons(new ArrayList<>(coupons));
        food.setFoodName(request.getFoodName());
        food.setPreparationTime(request.getPreparationTime());
        food.setFoodNote(request.getFoodNote());
        food.setPrice(request.getPrice());
        food.setDiscountPrice(discountPrice);
        food.setServiceFee(
                request.getServiceFee() != null
                        ? request.getServiceFee()
                        : BigDecimal.ZERO
        );

        if (deletedImages != null && !deletedImages.isEmpty()
                && food.getImages() != null) {
            List<FoodImage> imagesToDelete = food.getImages().stream()
                    .filter(image -> deletedImages.contains(image.getImageUrl()))
                    .toList();

            for (FoodImage image : imagesToDelete) {
                fileStorageService.deleteFoodImage(image.getImageUrl());
                food.getImages().remove(image);
            }
        }

        if (images != null && !images.isEmpty()) {
            if (food.getImages() == null) {
                food.setImages(new ArrayList<>());
            }

            for (int i = images.size() - 1; i >= 0; i--) {
                MultipartFile image = images.get(i);

                if (image == null || image.isEmpty()) {
                    continue;
                }

                String imageUrl = fileStorageService.saveFoodImage(
                        food.getId(),
                        image
                );

                FoodImage foodImage = FoodImage.builder()
                        .food(food)
                        .imageUrl(imageUrl)
                        .isPrimary(food.getImages().isEmpty())
                        .build();

                food.getImages().add(foodImage);
            }
        }

        return mapToResponse(foodRepository.save(food));
    }

    @Override
    @Transactional
    public void deleteFood(UUID merchantId, Long foodId) {
        Merchant merchant = getApprovedMerchant(merchantId);

        Food food = foodRepository
                .findByIdAndMerchant_IdAndDeletedAtIsNull(
                        foodId,
                        merchantId
                )
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy món ăn"));

        food.setMerchant(merchant);
        food.setDeletedAt(LocalDateTime.now());

        foodRepository.save(food);
    }

    private Merchant getApprovedMerchant(UUID merchantId) {
        Merchant merchant = merchantRepository
                .findById(merchantId)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy Merchant"));

        if (merchant.getMerchantStatus() != MerchantStatus.APPROVED) {
            throw new RuntimeException(
                    "Merchant chưa được Admin phê duyệt"
            );
        }

        return merchant;
    }

    private FoodResponse mapToResponse(Food food) {
        FoodResponse response = new FoodResponse();

        response.setId(food.getId());

        if (food.getMerchantAddress() != null) {
            response.setMerchantAddressId(
                    food.getMerchantAddress().getId()
            );
            response.setMerchantAddress(
                    food.getMerchantAddress().getMerchantAddress()
            );
            response.setMerchantOpenTime(
                    food.getMerchantAddress().getMerchantOpenTime()
            );
            response.setMerchantCloseTime(
                    food.getMerchantAddress().getMerchantCloseTime()
            );
        }

        response.setFoodName(food.getFoodName());
        response.setPreparationTime(food.getPreparationTime());
        response.setFoodNote(food.getFoodNote());
        response.setPrice(food.getPrice());
        response.setDiscountPrice(food.getDiscountPrice());
        response.setServiceFee(food.getServiceFee());
        response.setViews(food.getViews());
        response.setOrderCount(food.getOrderCount());
        response.setIsRecommended(food.getIsRecommended());

        if (food.getFoodCategories() != null) {
            response.setCategoryIds(
                    food.getFoodCategories()
                            .stream()
                            .map(FoodCategory::getId)
                            .toList()
            );
            response.setCategoryNames(
                    food.getFoodCategories()
                            .stream()
                            .map(FoodCategory::getCategoryName)
                            .toList()
            );
        }

        if (food.getTags() != null) {
            response.setTagIds(
                    food.getTags()
                            .stream()
                            .map(Tag::getId)
                            .toList()
            );
            response.setTagNames(
                    food.getTags()
                            .stream()
                            .map(Tag::getTagName)
                            .toList()
            );
        }

        if (food.getCoupons() != null) {
            response.setCouponIds(
                    food.getCoupons()
                            .stream()
                            .map(Coupon::getId)
                            .toList()
            );
            response.setCouponCodes(
                    food.getCoupons()
                            .stream()
                            .map(Coupon::getCouponCode)
                            .toList()
            );
        }

        if (food.getImages() != null) {
            response.setImageUrls(
                    food.getImages()
                            .stream()
                            .map(FoodImage::getImageUrl)
                            .toList()
            );
        }

        response.setCreatedAt(food.getCreatedAt());
        response.setUpdatedAt(food.getUpdatedAt());

        return response;
    }

    @Override
    @Transactional
    public void toggleRecommendation(UUID merchantId, Long foodId) {
        Food food = foodRepository
                .findByIdAndMerchant_IdAndDeletedAtIsNull(
                        foodId,
                        merchantId
                )
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy món ăn"));

        food.setIsRecommended(
                !Boolean.TRUE.equals(food.getIsRecommended())
        );

        foodRepository.save(food);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FoodResponse> searchMerchantFoodsByKeyword(
            UUID merchantId,
            String keyword,
            Pageable pageable) {

        if (!merchantRepository.existsById(merchantId)) {
            throw new RuntimeException("Không tìm thấy Merchant");
        }

        String cleanKeyword = keyword != null
                ? keyword.trim()
                : "";

        return foodRepository
                .searchMerchantFoodsByKeyword(
                        merchantId,
                        cleanKeyword,
                        pageable
                )
                .map(this::mapToResponse);
    }

    private BigDecimal calculateDiscountPrice(
            BigDecimal price,
            String discountType,
            BigDecimal discountValue) {

        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Giá món ăn phải lớn hơn 0");
        }

        if (discountType == null
                || discountType.isBlank()
                || discountValue == null) {
            return null;
        }

        if (discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Giá trị khuyến mãi phải lớn hơn 0");
        }

        BigDecimal result;

        switch (discountType.toUpperCase()) {
            case "FIXED_PRICE" -> {
                if (discountValue.compareTo(price) >= 0) {
                    throw new RuntimeException(
                            "Giá sau giảm phải nhỏ hơn giá món ăn");
                }

                result = discountValue;
            }

            case "PERCENT" -> {
                if (discountValue.compareTo(BigDecimal.valueOf(100)) >= 0) {
                    throw new RuntimeException(
                            "Phần trăm giảm phải nhỏ hơn 100%");
                }

                BigDecimal discountAmount = price
                        .multiply(discountValue)
                        .divide(
                                BigDecimal.valueOf(100),
                                2,
                                RoundingMode.HALF_UP
                        );

                result = price.subtract(discountAmount);
            }

            default -> throw new RuntimeException(
                    "Loại khuyến mãi không hợp lệ. " +
                            "Chỉ hỗ trợ FIXED_PRICE hoặc PERCENT"
            );
        }

        if (result.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException(
                    "Giá sau khuyến mãi phải lớn hơn 0");
        }

        return result.setScale(2, RoundingMode.HALF_UP);
    }
}