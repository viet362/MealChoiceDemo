package vn.codegyme.meal_choice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.codegyme.meal_choice.entity.MerchantSettlement;
import vn.codegyme.meal_choice.entity.SettlementStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantSettlementRepository extends JpaRepository<MerchantSettlement, Long> {

    @Query("""
        SELECT COALESCE(SUM(s.netRevenue), 0)
        FROM MerchantSettlement s
        WHERE s.merchant.id = :merchantId
          AND s.status = vn.codegyme.meal_choice.entity.SettlementStatus.CONFIRMED
        """)
    BigDecimal getTotalConfirmedSettlementRevenue(@Param("merchantId") UUID merchantId);

    @Query("SELECT s FROM MerchantSettlement s WHERE s.merchant.id = :merchantId AND s.periodKey = :periodKey")
    Optional<MerchantSettlement> findByMerchant_IdAndPeriodKey(@Param("merchantId") UUID merchantId, @Param("periodKey") String periodKey);

    @Query("SELECT s FROM MerchantSettlement s WHERE s.merchant.id = :merchantId ORDER BY s.startDate DESC")
    List<MerchantSettlement> findByMerchant_IdOrderByStartDateDesc(@Param("merchantId") UUID merchantId);

    @Query("SELECT s FROM MerchantSettlement s WHERE s.merchant.id = :merchantId AND s.status = :status ORDER BY s.startDate DESC")
    List<MerchantSettlement> findByMerchant_IdAndStatusOrderByStartDateDesc(@Param("merchantId") UUID merchantId, @Param("status") SettlementStatus status);

    @Query("""
        SELECT s FROM MerchantSettlement s
        WHERE s.merchant.id = :merchantId
          AND s.status = vn.codegyme.meal_choice.entity.SettlementStatus.CONFIRMED
          AND s.periodKey <> :periodKey
          AND s.startDate < :endDate
          AND s.endDate > :startDate
        ORDER BY s.startDate ASC
        """)
    List<MerchantSettlement> findOverlappingConfirmedSettlements(
            @Param("merchantId") UUID merchantId,
            @Param("periodKey") String periodKey,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
        SELECT s FROM MerchantSettlement s
        WHERE s.status = vn.codegyme.meal_choice.entity.SettlementStatus.PENDING_CONFIRMATION
          AND s.endDate <= :cutoff
        """)
    List<MerchantSettlement> findPendingSettlementsEligibleForAutoConfirm(@Param("cutoff") LocalDateTime cutoff);

    @Query("""
        SELECT s FROM MerchantSettlement s
        JOIN FETCH s.merchant m
        WHERE (:status IS NULL OR s.status = :status)
          AND (:keyword IS NULL OR LOWER(m.merchantRestaurantName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(m.merchantEmail) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(s.periodKey) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY s.startDate DESC, s.createdAt DESC
        """)
    List<MerchantSettlement> searchSettlementsForAdmin(@Param("status") SettlementStatus status, @Param("keyword") String keyword);
}
