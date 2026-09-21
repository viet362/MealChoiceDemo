package vn.codegyme.meal_choice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.codegyme.meal_choice.entity.Food;
import vn.codegyme.meal_choice.entity.FoodCategory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FoodRepository extends JpaRepository<Food, Long> {
    boolean existsByMerchantAddress_Id(UUID merchantAddressId);

    List<Food> findByMerchant_IdAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID merchantId
    );

    // Lấy danh sách món của Merchant có phân trang
    Page<Food> findByMerchant_IdAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID merchantId,
            Pageable pageable
    );

    // Lấy một món thuộc Merchant
    Optional<Food> findByIdAndMerchant_IdAndDeletedAtIsNull(
            Long foodId,
            UUID merchantId
    );

    // Kiểm tra món có thuộc Merchant không
    boolean existsByIdAndMerchant_IdAndDeletedAtIsNull(
            Long foodId,
            UUID merchantId
    );

    // Lấy tất cả món đang hoạt động
    Page<Food> findAllByIsActiveTrueAndDeletedAtIsNullOrderByIdDesc(
            Pageable pageable
    );

    @Query("""
            SELECT DISTINCT f
            FROM Food f
            JOIN f.foodCategories c
            WHERE f.isActive = true
              AND f.deletedAt IS NULL
              AND c.id = :categoryId
            ORDER BY f.id DESC
            """)
    Page<Food> findAllByCategoryId(
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    // Tìm kiếm theo tên món
    @Query("""
            SELECT f
            FROM Food f
            WHERE f.isActive = true
              AND f.deletedAt IS NULL
              AND LOWER(f.foodName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY f.id DESC
            """)
    Page<Food> searchByKeyword(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // Tìm kiếm theo tên món và danh mục
    @Query("""
            SELECT DISTINCT f
            FROM Food f
            JOIN f.foodCategories c
            WHERE f.isActive = true
              AND f.deletedAt IS NULL
              AND c.id = :categoryId
              AND LOWER(f.foodName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY f.id DESC
            """)
    Page<Food> searchByKeywordAndCategory(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    // Lấy món giảm giá nhiều nhất
    @Query("""
            SELECT f
            FROM Food f
            WHERE f.isActive = true
              AND f.deletedAt IS NULL
              AND f.discountPrice IS NOT NULL
              AND f.discountPrice < f.price
            ORDER BY (f.price - f.discountPrice) DESC, f.id DESC
            """)
    Page<Food> findAllTopDiscounts(
            Pageable pageable
    );

    // Lấy món giảm giá theo danh mục
    @Query("""
            SELECT DISTINCT f
            FROM Food f
            JOIN f.foodCategories c
            WHERE f.isActive = true
              AND f.deletedAt IS NULL
              AND c.id = :categoryId
              AND f.discountPrice IS NOT NULL
              AND f.discountPrice < f.price
            ORDER BY (f.price - f.discountPrice) DESC, f.id DESC
            """)
    Page<Food> findTopDiscountsByCategory(
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    // Lấy món theo khu vực
    @Query("""
            SELECT DISTINCT f
            FROM Food f
            JOIN f.merchantAddress a
            WHERE f.isActive = true
              AND f.deletedAt IS NULL
              AND LOWER(a.merchantAddress)
                  LIKE LOWER(CONCAT('%', :location, '%'))
            ORDER BY f.id DESC
            """)
    Page<Food> findByLocationNearUser(
            @Param("location") String location,
            Pageable pageable
    );

    // Lấy món theo khu vực và danh mục
    @Query("""
            SELECT DISTINCT f
            FROM Food f
            JOIN f.merchantAddress a
            JOIN f.foodCategories c
            WHERE f.isActive = true
              AND f.deletedAt IS NULL
              AND c.id = :categoryId
              AND LOWER(a.merchantAddress)
                  LIKE LOWER(CONCAT('%', :location, '%'))
            ORDER BY f.id DESC
            """)
    Page<Food> findByLocationNearUserAndCategory(
            @Param("location") String location,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    // Lấy danh sách món của Merchant đang hoạt động
    Page<Food> findAllByMerchant_IdAndIsActiveTrueAndDeletedAtIsNullOrderByIdDesc(
            UUID merchantId,
            Pageable pageable
    );

    // Lấy danh sách món đang hoạt động trong thành phố
    @Query("""
            SELECT DISTINCT f
            FROM Food f
            JOIN f.merchantAddress a
            WHERE f.isActive = true
              AND f.deletedAt IS NULL
              AND LOWER(a.merchantAddress)
                  LIKE LOWER(CONCAT('%', :city, '%'))
            """)
    List<Food> findAllActiveInCity(
            @Param("city") String city
    );

    // Lấy ID các món user đã thích
    @Query(value = "SELECT food_id FROM food_likes WHERE user_id = :userId", nativeQuery = true)
    List<Long> findLikedFoodIdsByUserId(
            @Param("userId") String userId
    );

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = "INSERT IGNORE INTO food_likes (food_id, user_id) VALUES (:foodId, :userId)", nativeQuery = true)
    int likeFood(@Param("foodId") Long foodId, @Param("userId") String userId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = "DELETE FROM food_likes WHERE food_id = :foodId AND user_id = :userId", nativeQuery = true)
    int unlikeFood(@Param("foodId") Long foodId, @Param("userId") String userId);
    // Lấy món mới nhất theo danh mục
    @Query("""
            SELECT DISTINCT f
            FROM Food f
            JOIN f.foodCategories c
            WHERE f.isActive = true
              AND f.deletedAt IS NULL
              AND c.id = :categoryId
            ORDER BY f.id DESC
            """)
    List<Food> findLatestFoodByCategoryId(
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    // Lấy món ăn từ đối tác chọn lọc
    @Query("""
            SELECT DISTINCT f
            FROM Food f
            JOIN f.merchant m
            WHERE f.isActive = true
              AND f.deletedAt IS NULL
              AND (m.trustedPartner = true OR m.merchantStatus = vn.codegyme.meal_choice.entity.MerchantStatus.APPROVED)
            ORDER BY f.isRecommended DESC, f.orderCount DESC, f.views DESC, f.id DESC
            """)
    Page<Food> findTrustedFoods(
            Pageable pageable
    );

    // Lấy món đối tác chọn lọc theo danh mục
    @Query("""
            SELECT DISTINCT f
            FROM Food f
            JOIN f.merchant m
            JOIN f.foodCategories c
            WHERE f.isActive = true
              AND f.deletedAt IS NULL
              AND (m.trustedPartner = true OR m.merchantStatus = vn.codegyme.meal_choice.entity.MerchantStatus.APPROVED)
              AND c.id = :categoryId
            ORDER BY f.isRecommended DESC, f.orderCount DESC, f.views DESC, f.id DESC
            """)
    Page<Food> findTrustedFoodsByCategory(
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    // Tìm kiếm gộp Tên món hoặc Địa chỉ dành riêng cho Merchant
    @Query("""
            SELECT DISTINCT f
            FROM Food f
            LEFT JOIN f.merchantAddress ma
            WHERE f.merchant.id = :merchantId
              AND f.deletedAt IS NULL
              AND (LOWER(f.foodName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(ma.merchantAddress) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY f.id DESC
            """)
    Page<Food> searchMerchantFoodsByKeyword(
            @Param("merchantId") UUID merchantId,
            @Param("keyword") String keyword,
            Pageable pageable
    );
    // Lấy danh sách danh mục phân biệt mà quán có món đang hoạt động
    @Query("""
            SELECT DISTINCT c
            FROM Food f
            JOIN f.foodCategories c
            WHERE f.merchant.id = :merchantId
              AND f.isActive = true
              AND f.deletedAt IS NULL
            ORDER BY c.id ASC
            """)
    List<FoodCategory> findDistinctCategoriesByMerchantId(
            @Param("merchantId") UUID merchantId
    );

    // Lấy danh sách danh mục có nhiều món ăn nhất của quán (Top thế mạnh quán)
    @Query("""
            SELECT c
            FROM Food f
            JOIN f.foodCategories c
            WHERE f.merchant.id = :merchantId
              AND f.isActive = true
              AND f.deletedAt IS NULL
            GROUP BY c.id, c.categoryName, c.categoryDescription, c.createdAt, c.updatedAt
            ORDER BY COUNT(f.id) DESC
            """)
    List<FoodCategory> findTopCategoriesByMerchantId(
            @Param("merchantId") UUID merchantId,
            Pageable pageable
    );

    // Lấy danh sách món đang hoạt động của quán theo danh mục
    @Query("""
            SELECT DISTINCT f
            FROM Food f
            JOIN f.foodCategories c
            WHERE f.merchant.id = :merchantId
              AND f.isActive = true
              AND f.deletedAt IS NULL
              AND c.id = :categoryId
            ORDER BY f.id DESC
            """)
    Page<Food> findActiveFoodsByMerchantAndCategory(
            @Param("merchantId") UUID merchantId,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    // Tìm kiếm món trong quán dành cho khách hàng
    @Query("""
            SELECT DISTINCT f
            FROM Food f
            LEFT JOIN f.foodCategories c
            WHERE f.merchant.id = :merchantId
              AND f.isActive = true
              AND f.deletedAt IS NULL
              AND LOWER(f.foodName) LIKE LOWER(CONCAT('%', :keyword, '%'))
              AND (:categoryId IS NULL OR c.id = :categoryId)
            ORDER BY f.id DESC
            """)
    Page<Food> searchCustomerFoodsInMerchant(
            @Param("merchantId") UUID merchantId,
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    // Đếm số món đang hoạt động của quán
    long countByMerchant_IdAndIsActiveTrueAndDeletedAtIsNull(UUID merchantId);
}