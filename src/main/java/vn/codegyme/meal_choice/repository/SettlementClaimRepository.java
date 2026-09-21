package vn.codegyme.meal_choice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.codegyme.meal_choice.entity.SettlementClaim;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SettlementClaimRepository extends JpaRepository<SettlementClaim, Long> {

    Optional<SettlementClaim> findBySettlement_Id(Long settlementId);

    Optional<SettlementClaim> findTopBySettlement_IdOrderByCreatedAtDesc(Long settlementId);

    List<SettlementClaim> findByMerchant_IdOrderByCreatedAtDesc(UUID merchantId);

    List<SettlementClaim> findByStatusOrderByCreatedAtDesc(String status);

    List<SettlementClaim> findAllByOrderByCreatedAtDesc();
}
