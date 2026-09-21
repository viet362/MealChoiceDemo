package vn.codegyme.meal_choice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.codegyme.meal_choice.entity.TrustedPartnerRequest;
import vn.codegyme.meal_choice.entity.TrustedPartnerRequestStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrustedPartnerRequestRepository
        extends JpaRepository<TrustedPartnerRequest, Long> {

    Optional<TrustedPartnerRequest> findFirstByMerchant_IdAndStatusOrderByCreatedAtDesc(
            UUID merchantId,
            TrustedPartnerRequestStatus status
    );

    Optional<TrustedPartnerRequest> findFirstByMerchant_IdOrderByCreatedAtDesc(
            UUID merchantId
    );

    boolean existsByMerchant_IdAndStatus(
            UUID merchantId,
            TrustedPartnerRequestStatus status
    );

    List<TrustedPartnerRequest> findByStatusOrderByCreatedAtDesc(
            TrustedPartnerRequestStatus status
    );
}