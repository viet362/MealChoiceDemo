package vn.codegyme.meal_choice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.codegyme.meal_choice.entity.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface MerchantPayoutRequestRepository
        extends JpaRepository<MerchantPayoutRequest, UUID> {


    List<MerchantPayoutRequest>
    findByMerchant_IdOrderByCreatedAtDesc(
            UUID merchantId
    );


    List<MerchantPayoutRequest>
    findAllByOrderByCreatedAtDesc();


    List<MerchantPayoutRequest>
    findByStatusOrderByCreatedAtDesc(
            PayoutRequestStatus status
    );


    boolean existsByMerchant_IdAndStatus(
            UUID merchantId,
            PayoutRequestStatus status
    );


    @Query("""
            SELECT COALESCE(SUM(r.amount), 0)
            FROM MerchantPayoutRequest r
            WHERE r.merchant.id = :merchantId
            AND r.status = vn.codegyme.meal_choice.entity.PayoutRequestStatus.COMPLETED
            """)
    BigDecimal getTotalPaidAmount(
            @Param("merchantId") UUID merchantId
    );

    @Query("""
            SELECT COALESCE(SUM(r.amount), 0)
            FROM MerchantPayoutRequest r
            WHERE r.merchant.id = :merchantId
            AND r.status = vn.codegyme.meal_choice.entity.PayoutRequestStatus.PENDING
            """)
    BigDecimal getTotalPendingAmount(
            @Param("merchantId") UUID merchantId
    );
}
