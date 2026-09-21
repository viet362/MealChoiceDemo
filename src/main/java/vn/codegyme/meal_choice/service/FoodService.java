package vn.codegyme.meal_choice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.codegyme.meal_choice.dto.food.FoodCreateRequest;
import vn.codegyme.meal_choice.dto.food.FoodResponse;
import vn.codegyme.meal_choice.dto.food.FoodUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface FoodService {

    // Thêm món ăn
    FoodResponse createFood(
            UUID merchantId,
            FoodCreateRequest request,
            List<MultipartFile> images
    );

    // Lấy danh sách món ăn của Merchant
    Page<FoodResponse> getFoods(UUID merchantId, Pageable pageable);

    // Lấy chi tiết món ăn
    FoodResponse getFood(UUID merchantId, Long foodId);

    // Cập nhật món
    @Transactional
    FoodResponse updateFood(
            UUID merchantId,
            Long foodId,
            FoodUpdateRequest request,
            List<MultipartFile> images,
            List<String> deletedImages);

    // Xóa mềm món ăn
    void deleteFood(UUID merchantId, Long foodId);

    // Bật hoặc tắt đề xuất món ăn
    void toggleRecommendation(UUID merchantId, Long foodId);

    // Tìm kiếm món ăn theo tên hoặc địa chỉ
    Page<FoodResponse> searchMerchantFoodsByKeyword(
            UUID merchantId,
            String keyword,
            Pageable pageable
    );
}