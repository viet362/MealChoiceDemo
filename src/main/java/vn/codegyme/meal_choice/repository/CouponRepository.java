package vn.codegyme.meal_choice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.codegyme.meal_choice.entity.Coupon;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    List<Coupon> findAllByMerchant_IdOrderByCreatedAtDesc(UUID merchantId);

    List<Coupon> findAllByMerchant_IdAndIsActiveTrueOrderByCreatedAtDesc(UUID merchantId);

    Optional<Coupon> findByIdAndMerchant_Id(Long id, UUID merchantId);

    List<Coupon> findAllByIdInAndMerchant_Id(
            List<Long> ids,
            UUID merchantId
    );

    boolean existsByMerchant_IdAndCouponCode(
            UUID merchantId,
            String couponCode
    );

    boolean existsByMerchant_IdAndCouponCodeAndIdNot(
            UUID merchantId,
            String couponCode,
            Long id
    );

    Optional<Coupon> findByMerchant_IdAndCouponCode(UUID merchantId, String couponCode);

    @Query("""
            SELECT DISTINCT c
            FROM Coupon c
            WHERE c.merchant.id = :merchantId
              AND c.isActive = true
              AND (c.startAt IS NULL OR c.startAt <= CURRENT_TIMESTAMP)
              AND (c.endAt IS NULL OR c.endAt >= CURRENT_TIMESTAMP)
              AND (c.usageLimit IS NULL OR c.usedCount < c.usageLimit)
            ORDER BY c.createdAt DESC
            """)
    List<Coupon> findAllActiveByMerchantId(@Param("merchantId") UUID merchantId);

    // Lấy các Coupon đang hoạt động được áp dụng cho món ăn
    @Query("""
            SELECT c
            FROM Coupon c
            JOIN c.foods f
            WHERE f.id = :foodId
              AND c.isActive = true
            ORDER BY c.createdAt DESC
            """)
    List<Coupon> findActiveCouponsByFoodId(@Param("foodId") Long foodId);
}