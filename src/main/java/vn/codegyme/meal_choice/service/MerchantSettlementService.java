package vn.codegyme.meal_choice.service;

import org.springframework.web.multipart.MultipartFile;
import vn.codegyme.meal_choice.dto.settlement.AdminSettlementItemDTO;
import vn.codegyme.meal_choice.dto.settlement.AdminSettlementStatsDTO;
import vn.codegyme.meal_choice.dto.settlement.SettlementOverviewDTO;
import vn.codegyme.meal_choice.dto.settlement.SettlementPeriodOptionDTO;
import vn.codegyme.meal_choice.entity.SettlementClaimReason;

import java.util.List;
import java.util.UUID;

public interface MerchantSettlementService {

    List<SettlementPeriodOptionDTO> getAvailablePeriods(UUID merchantId);

    List<SettlementPeriodOptionDTO> getAvailablePeriods(UUID merchantId, String periodType);

    SettlementOverviewDTO getSettlementOverview(UUID merchantId, String periodKey, String periodType);

    SettlementOverviewDTO confirmSettlement(UUID merchantId, Long settlementId);

    SettlementOverviewDTO claimSettlement(UUID merchantId, Long settlementId, SettlementClaimReason reason, String description, MultipartFile evidenceImage);

    int autoConfirmPendingSettlements(int daysThreshold);

    List<AdminSettlementItemDTO> getAdminSettlements(String status, String keyword);

    AdminSettlementStatsDTO getAdminSettlementStats();

    AdminSettlementItemDTO resolveClaim(Long claimId, java.math.BigDecimal adjustmentAmount, String adminNote);

    AdminSettlementItemDTO rejectClaim(Long claimId, String adminNote);
}
