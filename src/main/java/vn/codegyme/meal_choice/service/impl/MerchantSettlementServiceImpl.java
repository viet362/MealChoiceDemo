package vn.codegyme.meal_choice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.codegyme.meal_choice.dto.settlement.SettlementOrderDTO;
import vn.codegyme.meal_choice.dto.settlement.SettlementOverviewDTO;
import vn.codegyme.meal_choice.dto.settlement.SettlementPeriodOptionDTO;
import vn.codegyme.meal_choice.entity.*;
import vn.codegyme.meal_choice.repository.MerchantRepository;
import vn.codegyme.meal_choice.repository.MerchantSettlementRepository;
import vn.codegyme.meal_choice.repository.OrderRepository;
import vn.codegyme.meal_choice.repository.SettlementClaimRepository;
import vn.codegyme.meal_choice.service.FileStorageService;
import vn.codegyme.meal_choice.service.MerchantSettlementService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantSettlementServiceImpl implements MerchantSettlementService {

    private static final BigDecimal THRESHOLD_200M = new BigDecimal("200000000");
    // 0.001% = 0.000010
    private static final BigDecimal RATE_BELOW_200M = new BigDecimal("0.000010");
    // 0.0005% = 0.000005
    private static final BigDecimal RATE_ABOVE_200M = new BigDecimal("0.000005");

    private final MerchantSettlementRepository settlementRepository;
    private final SettlementClaimRepository claimRepository;
    private final OrderRepository orderRepository;
    private final MerchantRepository merchantRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public List<SettlementPeriodOptionDTO> getAvailablePeriods(UUID merchantId) {
        return getAvailablePeriods(merchantId, "MONTH");
    }

    @Override
    @Transactional(readOnly = true)
    public List<SettlementPeriodOptionDTO> getAvailablePeriods(UUID merchantId, String periodType) {
        List<SettlementPeriodOptionDTO> options = new ArrayList<>();
        boolean isWeekly = "WEEK".equalsIgnoreCase(periodType);

        if (isWeekly) {
            LocalDate today = LocalDate.now();
            LocalDate currentMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

            for (int i = 0; i < 8; i++) {
                LocalDate monday = currentMonday.minusWeeks(i);
                LocalDate sunday = monday.plusDays(6);
                LocalDateTime startDate = monday.atStartOfDay();
                LocalDateTime endDate = monday.plusDays(7).atStartOfDay();

                int weekNum = monday.get(WeekFields.ISO.weekOfWeekBasedYear());
                int year = monday.get(WeekFields.ISO.weekBasedYear());
                String periodKey = String.format("%d-W%02d", year, weekNum);
                String label = String.format("Tuần %02d/%d (%02d/%02d - %02d/%02d)",
                        weekNum, year,
                        monday.getDayOfMonth(), monday.getMonthValue(),
                        sunday.getDayOfMonth(), sunday.getMonthValue());

                boolean isEnded = !LocalDateTime.now().isBefore(endDate);

                Optional<MerchantSettlement> existingOpt =
                        settlementRepository.findByMerchant_IdAndPeriodKey(merchantId, periodKey);

                SettlementStatus status;
                if (existingOpt.isPresent()) {
                    status = existingOpt.get().getStatus();
                } else {
                    status = isEnded ? SettlementStatus.PENDING_CONFIRMATION : SettlementStatus.IN_PROGRESS;
                }

                options.add(SettlementPeriodOptionDTO.builder()
                        .periodKey(periodKey)
                        .label(label)
                        .periodType("WEEK")
                        .startDate(startDate)
                        .endDate(endDate)
                        .status(status.name())
                        .statusDisplayName(status.getDisplayName())
                        .statusBadgeClass(status.getBadgeClass())
                        .build());
            }
        } else {
            YearMonth currentYearMonth = YearMonth.now();

            for (int i = 0; i < 6; i++) {
                YearMonth ym = currentYearMonth.minusMonths(i);
                String periodKey = ym.toString();
                String label = String.format("Tháng %02d/%d", ym.getMonthValue(), ym.getYear());

                LocalDateTime startDate = ym.atDay(1).atStartOfDay();
                LocalDateTime endDate = ym.plusMonths(1).atDay(1).atStartOfDay();

                boolean isEnded = !LocalDateTime.now().isBefore(endDate);

                Optional<MerchantSettlement> existingOpt =
                        settlementRepository.findByMerchant_IdAndPeriodKey(merchantId, periodKey);

                SettlementStatus status;
                if (existingOpt.isPresent()) {
                    status = existingOpt.get().getStatus();
                } else {
                    status = isEnded ? SettlementStatus.PENDING_CONFIRMATION : SettlementStatus.IN_PROGRESS;
                }

                options.add(SettlementPeriodOptionDTO.builder()
                        .periodKey(periodKey)
                        .label(label)
                        .periodType("MONTH")
                        .startDate(startDate)
                        .endDate(endDate)
                        .status(status.name())
                        .statusDisplayName(status.getDisplayName())
                        .statusBadgeClass(status.getBadgeClass())
                        .build());
            }
        }

        return options;
    }

    @Override
    @Transactional
    public SettlementOverviewDTO getSettlementOverview(UUID merchantId, String periodKey, String periodType) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin đối tác"));

        LocalDateTime startDate;
        LocalDateTime endDate;
        String periodLabel;
        boolean isWeekly = "WEEK".equalsIgnoreCase(periodType) || (periodKey != null && periodKey.contains("-W"));
        String actualPeriodType = isWeekly ? "WEEK" : "MONTH";

        if (isWeekly) {
            int year;
            int weekNum;
            if (periodKey != null && periodKey.contains("-W")) {
                try {
                    String[] parts = periodKey.split("-W");
                    year = Integer.parseInt(parts[0]);
                    weekNum = Integer.parseInt(parts[1]);
                } catch (Exception e) {
                    LocalDate now = LocalDate.now();
                    year = now.get(WeekFields.ISO.weekBasedYear());
                    weekNum = now.get(WeekFields.ISO.weekOfWeekBasedYear());
                    periodKey = String.format("%d-W%02d", year, weekNum);
                }
            } else {
                LocalDate now = LocalDate.now();
                year = now.get(WeekFields.ISO.weekBasedYear());
                weekNum = now.get(WeekFields.ISO.weekOfWeekBasedYear());
                periodKey = String.format("%d-W%02d", year, weekNum);
            }

            LocalDate monday = LocalDate.of(year, 1, 4)
                    .with(WeekFields.ISO.weekOfWeekBasedYear(), weekNum)
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate sunday = monday.plusDays(6);

            startDate = monday.atStartOfDay();
            endDate = monday.plusDays(7).atStartOfDay();
            periodLabel = String.format("Tuần %02d/%d (%02d/%02d - %02d/%02d)",
                    weekNum, year,
                    monday.getDayOfMonth(), monday.getMonthValue(),
                    sunday.getDayOfMonth(), sunday.getMonthValue());
        } else {
            YearMonth ym;
            if (periodKey == null || periodKey.isBlank() || periodKey.contains("-W")) {
                ym = YearMonth.now();
                periodKey = ym.toString();
            } else {
                try {
                    ym = YearMonth.parse(periodKey);
                } catch (Exception e) {
                    ym = YearMonth.now();
                    periodKey = ym.toString();
                }
            }
            startDate = ym.atDay(1).atStartOfDay();
            endDate = ym.plusMonths(1).atDay(1).atStartOfDay();
            periodLabel = String.format("Tháng %02d/%d", ym.getMonthValue(), ym.getYear());
        }

        boolean isEnded = !LocalDateTime.now().isBefore(endDate);

        // Lấy các đơn hàng COMPLETED trong kỳ (lọc theo thời điểm hoàn thành đơn)
        List<Order> completedOrders = orderRepository.findCompletedOrdersInPeriod(merchantId, startDate, endDate);

        BigDecimal grossRevenue = completedOrders.stream()
                .map(Order::getSubtotalPrice)
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDiscount = completedOrders.stream()
                .map(Order::getDiscountAmount)
                .filter(d -> d != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Xác định tỷ lệ phí chiết khấu sàn
        BigDecimal commissionRate = grossRevenue.compareTo(THRESHOLD_200M) >= 0 ? RATE_ABOVE_200M : RATE_BELOW_200M;

        // Tổng phí chiết khấu sàn
        BigDecimal totalCommissionFee = grossRevenue.multiply(commissionRate).setScale(0, RoundingMode.HALF_UP);

        // Doanh thu thực nhận ban đầu = Giá sản phẩm - Khuyến mãi - Chiết khấu sàn
        BigDecimal baseNetRevenue = grossRevenue.subtract(totalDiscount).subtract(totalCommissionFee);
        if (baseNetRevenue.compareTo(BigDecimal.ZERO) < 0) {
            baseNetRevenue = BigDecimal.ZERO;
        }

        long totalOrders = completedOrders.size();

        // Tìm hoặc tạo mới MerchantSettlement
        MerchantSettlement settlement = settlementRepository.findByMerchant_IdAndPeriodKey(merchantId, periodKey)
                .orElse(null);

        SettlementStatus initialStatus = isEnded ? SettlementStatus.PENDING_CONFIRMATION : SettlementStatus.IN_PROGRESS;

        if (settlement == null) {
            settlement = MerchantSettlement.builder()
                    .merchant(merchant)
                    .periodKey(periodKey)
                    .periodType(actualPeriodType)
                    .startDate(startDate)
                    .endDate(endDate)
                    .totalGrossRevenue(grossRevenue)
                    .totalDiscount(totalDiscount)
                    .commissionRate(commissionRate)
                    .totalCommissionFee(totalCommissionFee)
                    .netRevenue(baseNetRevenue)
                    .adjustmentAmount(BigDecimal.ZERO)
                    .totalOrders(totalOrders)
                    .status(initialStatus)
                    .build();
            settlement = settlementRepository.save(settlement);
        } else if (settlement.getStatus() == SettlementStatus.PENDING_CONFIRMATION || settlement.getStatus() == SettlementStatus.IN_PROGRESS) {
            // Cập nhật số liệu mới nhất khi đang chờ xác nhận hoặc đang diễn ra
            settlement.setTotalGrossRevenue(grossRevenue);
            settlement.setTotalDiscount(totalDiscount);
            settlement.setCommissionRate(commissionRate);
            settlement.setTotalCommissionFee(totalCommissionFee);
            BigDecimal adj = settlement.getAdjustmentAmount() != null ? settlement.getAdjustmentAmount() : BigDecimal.ZERO;
            settlement.setNetRevenue(baseNetRevenue.add(adj));
            settlement.setTotalOrders(totalOrders);
            if (isEnded && settlement.getStatus() == SettlementStatus.IN_PROGRESS) {
                settlement.setStatus(SettlementStatus.PENDING_CONFIRMATION);
            } else if (!isEnded) {
                settlement.setStatus(SettlementStatus.IN_PROGRESS);
            }
            settlement = settlementRepository.save(settlement);
        }

        // Tạo danh sách chi tiết đơn hàng
        List<SettlementOrderDTO> orderDTOList = new ArrayList<>();
        for (Order order : completedOrders) {
            BigDecimal itemSubtotal = order.getSubtotalPrice() != null ? order.getSubtotalPrice() : BigDecimal.ZERO;
            BigDecimal itemDiscount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
            BigDecimal itemFee = itemSubtotal.multiply(commissionRate).setScale(0, RoundingMode.HALF_UP);
            BigDecimal itemNet = itemSubtotal.subtract(itemDiscount).subtract(itemFee);
            if (itemNet.compareTo(BigDecimal.ZERO) < 0) {
                itemNet = BigDecimal.ZERO;
            }

            orderDTOList.add(SettlementOrderDTO.builder()
                    .orderId(order.getId())
                    .orderCode(order.getOrderCode())
                    .createdAt(order.getCompletedAt() != null ? order.getCompletedAt() : (order.getUpdatedAt() != null ? order.getUpdatedAt() : order.getCreatedAt()))
                    .contactName(order.getContactName())
                    .contactPhone(order.getContactPhone())
                    .subtotalPrice(itemSubtotal)
                    .discountAmount(itemDiscount)
                    .commissionFee(itemFee)
                    .netAmount(itemNet)
                    .statusDisplayName(order.getStatus().getDisplayName())
                    .statusBadgeClass(order.getStatus().getBadgeClass())
                    .build());
        }

        // Lấy thông tin khiếu nại gần nhất nếu có
        String claimStatus = null;
        String claimReason = null;
        String claimDescription = null;
        String claimEvidenceUrl = null;
        String claimAdminNote = null;
        LocalDateTime claimCreatedAt = null;

        Optional<SettlementClaim> claimOpt = claimRepository.findTopBySettlement_IdOrderByCreatedAtDesc(settlement.getId());
        if (claimOpt.isPresent()) {
            SettlementClaim claim = claimOpt.get();
            claimStatus = claim.getStatus();
            claimReason = claim.getReason() != null ? claim.getReason().getDisplayName() : null;
            claimDescription = claim.getDescription();
            claimEvidenceUrl = claim.getEvidenceImageUrl();
            claimAdminNote = claim.getAdminNote();
            claimCreatedAt = claim.getCreatedAt();
        }

        String finalCommissionRateDisplay = settlement.getCommissionRate() != null
                && settlement.getCommissionRate().compareTo(RATE_ABOVE_200M) <= 0
                ? "0.0005%" : "0.001%";

        // Kiểm tra chống đối soát trùng lặp giữa Tuần và Tháng (Anti-Double-Settlement)
        List<MerchantSettlement> overlappingConfirmed =
                settlementRepository.findOverlappingConfirmedSettlements(
                        merchantId,
                        settlement.getPeriodKey(),
                        settlement.getStartDate(),
                        settlement.getEndDate()
                );

        boolean hasOverlap = !overlappingConfirmed.isEmpty();
        String overlapMessage = null;
        if (hasOverlap) {
            String overlappingPeriods = overlappingConfirmed.stream()
                    .map(s -> "WEEK".equalsIgnoreCase(s.getPeriodType()) ? "Tuần " + s.getPeriodKey() : "Tháng " + s.getPeriodKey())
                    .collect(Collectors.joining(", "));

            if ("MONTH".equalsIgnoreCase(settlement.getPeriodType())) {
                overlapMessage = "Kỳ này có khoảng thời gian đã được xác nhận chốt tiền ở kỳ " + overlappingPeriods 
                        + ". Để tránh tính tiền 2 lần, kỳ này chỉ dùng để xem báo cáo thống kê và không thể chốt số.";
            } else {
                overlapMessage = "Kỳ này có khoảng thời gian đã được xác nhận chốt tiền ở kỳ " + overlappingPeriods 
                        + ". Số tiền đã được chuyển, không thể chốt số lần 2.";
            }
        }

        BigDecimal adj = settlement.getAdjustmentAmount() != null ? settlement.getAdjustmentAmount() : BigDecimal.ZERO;
        boolean actionable = isEnded 
                && (settlement.getStatus() == SettlementStatus.PENDING_CONFIRMATION)
                && !hasOverlap;

        BigDecimal baseOriginalNetRevenue = settlement.getTotalGrossRevenue() != null
                ? settlement.getTotalGrossRevenue()
                        .subtract(settlement.getTotalDiscount() != null ? settlement.getTotalDiscount() : BigDecimal.ZERO)
                        .subtract(settlement.getTotalCommissionFee() != null ? settlement.getTotalCommissionFee() : BigDecimal.ZERO)
                : BigDecimal.ZERO;
        if (baseOriginalNetRevenue.compareTo(BigDecimal.ZERO) < 0) {
            baseOriginalNetRevenue = BigDecimal.ZERO;
        }

        return SettlementOverviewDTO.builder()
                .settlementId(settlement.getId())
                .periodKey(periodKey)
                .periodLabel(periodLabel)
                .periodType(settlement.getPeriodType())
                .startDate(startDate)
                .endDate(endDate)
                .totalGrossRevenue(settlement.getTotalGrossRevenue())
                .totalDiscount(settlement.getTotalDiscount())
                .commissionRate(settlement.getCommissionRate())
                .commissionRateDisplay(finalCommissionRateDisplay)
                .totalCommissionFee(settlement.getTotalCommissionFee())
                .originalNetRevenue(baseOriginalNetRevenue)
                .adjustmentAmount(adj)
                .netRevenue(settlement.getNetRevenue())
                .totalOrders(settlement.getTotalOrders())
                .status(settlement.getStatus().name())
                .statusDisplayName(settlement.getStatus().getDisplayName())
                .statusBadgeClass(settlement.getStatus().getBadgeClass())
                .confirmedAt(settlement.getConfirmedAt())
                .actionable(actionable)
                .isInProgress(!isEnded)
                .hasOverlap(hasOverlap)
                .overlapMessage(overlapMessage)
                .claimStatus(claimStatus)
                .claimReason(claimReason)
                .claimDescription(claimDescription)
                .claimEvidenceUrl(claimEvidenceUrl)
                .claimAdminNote(claimAdminNote)
                .claimCreatedAt(claimCreatedAt)
                .orders(orderDTOList)
                .build();
    }

    @Override
    @Transactional
    public SettlementOverviewDTO confirmSettlement(UUID merchantId, Long settlementId) {
        MerchantSettlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kỳ đối soát"));

        if (!settlement.getMerchant().getId().equals(merchantId)) {
            throw new RuntimeException("Bạn không có quyền thao tác trên kỳ đối soát này");
        }

        if (settlement.getStatus() != SettlementStatus.PENDING_CONFIRMATION 
                && settlement.getStatus() != SettlementStatus.IN_PROGRESS) {
            throw new IllegalStateException("Kỳ đối soát này đã được xử lý trước đó");
        }

        // Kiểm tra chống đối soát trùng lặp ở tầng Service
        List<MerchantSettlement> overlappingConfirmed =
                settlementRepository.findOverlappingConfirmedSettlements(
                        merchantId,
                        settlement.getPeriodKey(),
                        settlement.getStartDate(),
                        settlement.getEndDate()
                );

        if (!overlappingConfirmed.isEmpty()) {
            String overlappingPeriods = overlappingConfirmed.stream()
                    .map(s -> "WEEK".equalsIgnoreCase(s.getPeriodType()) ? "Tuần " + s.getPeriodKey() : "Tháng " + s.getPeriodKey())
                    .collect(Collectors.joining(", "));
            throw new IllegalStateException("Không thể xác nhận: Kỳ đối soát này có khoảng thời gian trùng lặp với kỳ " 
                    + overlappingPeriods + " đã được chốt chuyển tiền trước đó.");
        }

        settlement.setStatus(SettlementStatus.CONFIRMED);
        settlement.setConfirmedAt(LocalDateTime.now());
        settlementRepository.save(settlement);

        log.info("Merchant {} đã xác nhận đối soát kỳ {} thành công với số tiền thực nhận: {}",
                merchantId, settlement.getPeriodKey(), settlement.getNetRevenue());

        return getSettlementOverview(merchantId, settlement.getPeriodKey(), settlement.getPeriodType());
    }

    @Override
    @Transactional
    public SettlementOverviewDTO claimSettlement(
            UUID merchantId,
            Long settlementId,
            SettlementClaimReason reason,
            String description,
            MultipartFile evidenceImage) {

        MerchantSettlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kỳ đối soát"));

        if (!settlement.getMerchant().getId().equals(merchantId)) {
            throw new RuntimeException("Bạn không có quyền thao tác trên kỳ đối soát này");
        }

        if (settlement.getStatus() != SettlementStatus.PENDING_CONFIRMATION 
                && settlement.getStatus() != SettlementStatus.IN_PROGRESS) {
            throw new IllegalStateException("Kỳ đối soát này đã được xử lý trước đó");
        }

        List<MerchantSettlement> overlappingConfirmed =
                settlementRepository.findOverlappingConfirmedSettlements(
                        merchantId,
                        settlement.getPeriodKey(),
                        settlement.getStartDate(),
                        settlement.getEndDate()
                );

        if (!overlappingConfirmed.isEmpty()) {
            throw new IllegalStateException("Kỳ đối soát này có khoảng thời gian đã được chốt ở kỳ khác, không thể khiếu nại.");
        }

        String evidenceUrl = null;
        if (evidenceImage != null && !evidenceImage.isEmpty()) {
            evidenceUrl = fileStorageService.saveSettlementEvidenceImage(evidenceImage);
        }

        SettlementClaim claim = SettlementClaim.builder()
                .settlement(settlement)
                .merchant(settlement.getMerchant())
                .reason(reason)
                .description(description)
                .evidenceImageUrl(evidenceUrl)
                .status("PENDING")
                .build();

        claimRepository.save(claim);

        settlement.setStatus(SettlementStatus.DISPUTED);
        settlementRepository.save(settlement);

        log.warn("Merchant {} đã gửi khiếu nại kỳ đối soát {}. Lý do: {}",
                merchantId, settlement.getPeriodKey(), reason);

        return getSettlementOverview(merchantId, settlement.getPeriodKey(), settlement.getPeriodType());
    }

    @Override
    @Transactional
    public int autoConfirmPendingSettlements(int daysThreshold) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysThreshold);
        List<MerchantSettlement> eligible = settlementRepository.findPendingSettlementsEligibleForAutoConfirm(cutoff);

        int count = 0;
        for (MerchantSettlement settlement : eligible) {
            settlement.setStatus(SettlementStatus.CONFIRMED);
            settlement.setConfirmedAt(LocalDateTime.now());
            settlementRepository.save(settlement);
            count++;
            log.info("Auto-confirmed settlement ID={} for Merchant={} (Period={})",
                    settlement.getId(), settlement.getMerchant().getId(), settlement.getPeriodKey());
        }
        return count;
    }

    @Override
    @Transactional(readOnly = true)
    public List<vn.codegyme.meal_choice.dto.settlement.AdminSettlementItemDTO> getAdminSettlements(String statusStr, String keyword) {
        SettlementStatus status = null;
        if (statusStr != null && !statusStr.isBlank() && !"ALL".equalsIgnoreCase(statusStr)) {
            try {
                status = SettlementStatus.valueOf(statusStr.toUpperCase());
            } catch (Exception ignored) {}
        }

        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;

        List<MerchantSettlement> settlements = settlementRepository.searchSettlementsForAdmin(status, kw);

        return settlements.stream().map(s -> {
            Optional<SettlementClaim> claimOpt = claimRepository.findTopBySettlement_IdOrderByCreatedAtDesc(s.getId());
            boolean hasClaim = claimOpt.isPresent();
            SettlementClaim claim = claimOpt.orElse(null);

            String claimStatus = claim != null ? claim.getStatus() : null;
            String claimStatusDisplayName = null;
            String claimStatusBadgeClass = null;
            if ("PENDING".equalsIgnoreCase(claimStatus)) {
                claimStatusDisplayName = "Chờ xử lý";
                claimStatusBadgeClass = "bg-warning text-dark";
            } else if ("RESOLVED".equalsIgnoreCase(claimStatus)) {
                claimStatusDisplayName = "Đã duyệt";
                claimStatusBadgeClass = "bg-success";
            } else if ("REJECTED".equalsIgnoreCase(claimStatus)) {
                claimStatusDisplayName = "Đã từ chối";
                claimStatusBadgeClass = "bg-danger";
            }

            String merchantName = s.getMerchant() != null ? s.getMerchant().getMerchantRestaurantName() : "Cửa hàng không xác định";
            String merchantEmail = s.getMerchant() != null ? s.getMerchant().getMerchantEmail() : "";
            String merchantPhone = s.getMerchant() != null ? s.getMerchant().getMerchantPhone() : "";
            UUID merchantId = s.getMerchant() != null ? s.getMerchant().getId() : null;

            SettlementStatus sStatus = s.getStatus() != null ? s.getStatus() : SettlementStatus.PENDING_CONFIRMATION;

            BigDecimal adj = s.getAdjustmentAmount() != null ? s.getAdjustmentAmount() : BigDecimal.ZERO;
            boolean isEnded = !LocalDateTime.now().isBefore(s.getEndDate());

            return vn.codegyme.meal_choice.dto.settlement.AdminSettlementItemDTO.builder()
                    .settlementId(s.getId())
                    .merchantId(merchantId)
                    .merchantRestaurantName(merchantName)
                    .merchantEmail(merchantEmail)
                    .merchantPhone(merchantPhone)
                    .periodKey(s.getPeriodKey())
                    .periodLabel(formatPeriodLabel(s.getPeriodKey()))
                    .periodType(s.getPeriodType())
                    .startDate(s.getStartDate())
                    .endDate(s.getEndDate())
                    .totalGrossRevenue(s.getTotalGrossRevenue() != null ? s.getTotalGrossRevenue() : BigDecimal.ZERO)
                    .totalDiscount(s.getTotalDiscount() != null ? s.getTotalDiscount() : BigDecimal.ZERO)
                    .commissionRate(s.getCommissionRate() != null ? s.getCommissionRate() : BigDecimal.ZERO)
                    .totalCommissionFee(s.getTotalCommissionFee() != null ? s.getTotalCommissionFee() : BigDecimal.ZERO)
                    .adjustmentAmount(adj)
                    .netRevenue(s.getNetRevenue() != null ? s.getNetRevenue() : BigDecimal.ZERO)
                    .totalOrders(s.getTotalOrders() != null ? s.getTotalOrders() : 0L)
                    .status(sStatus.name())
                    .statusDisplayName(sStatus.getDisplayName())
                    .statusBadgeClass(sStatus.getBadgeClass())
                    .confirmedAt(s.getConfirmedAt())
                    .isInProgress(!isEnded)
                    .hasClaim(hasClaim)
                    .claimId(claim != null ? claim.getId() : null)
                    .claimReason(claim != null && claim.getReason() != null ? claim.getReason().name() : null)
                    .claimReasonDisplayName(claim != null && claim.getReason() != null ? claim.getReason().getDisplayName() : null)
                    .claimDescription(claim != null ? claim.getDescription() : null)
                    .claimEvidenceImageUrl(claim != null ? claim.getEvidenceImageUrl() : null)
                    .claimStatus(claimStatus)
                    .claimStatusDisplayName(claimStatusDisplayName)
                    .claimStatusBadgeClass(claimStatusBadgeClass)
                    .claimCreatedAt(claim != null ? claim.getCreatedAt() : null)
                    .claimAdminNote(claim != null ? claim.getAdminNote() : null)
                    .build();
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public vn.codegyme.meal_choice.dto.settlement.AdminSettlementStatsDTO getAdminSettlementStats() {
        List<MerchantSettlement> all = settlementRepository.findAll();

        long totalSettlements = all.size();
        long totalPendingConfirm = all.stream()
                .filter(s -> s.getStatus() == SettlementStatus.PENDING_CONFIRMATION).count();
        long totalConfirmed = all.stream()
                .filter(s -> s.getStatus() == SettlementStatus.CONFIRMED).count();
        long totalDisputed = all.stream()
                .filter(s -> s.getStatus() == SettlementStatus.DISPUTED).count();

        BigDecimal totalPendingPayout = all.stream()
                .filter(s -> s.getStatus() == SettlementStatus.CONFIRMED)
                .map(MerchantSettlement::getNetRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalActiveClaims = claimRepository.findByStatusOrderByCreatedAtDesc("PENDING").size();

        return vn.codegyme.meal_choice.dto.settlement.AdminSettlementStatsDTO.builder()
                .totalSettlements(totalSettlements)
                .totalPendingConfirm(totalPendingConfirm)
                .totalConfirmed(totalConfirmed)
                .totalDisputed(totalDisputed)
                .totalPendingPayoutAmount(totalPendingPayout)
                .totalActiveClaims(totalActiveClaims)
                .build();
    }

    @Override
    @Transactional
    public vn.codegyme.meal_choice.dto.settlement.AdminSettlementItemDTO resolveClaim(Long claimId, BigDecimal adjustmentAmount, String adminNote) {
        SettlementClaim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khiếu nại ID: " + claimId));

        BigDecimal adj = adjustmentAmount != null ? adjustmentAmount : BigDecimal.ZERO;
        claim.setStatus("RESOLVED");
        claim.setAdjustmentAmount(adj);
        claim.setAdminNote(adminNote != null && !adminNote.isBlank() ? adminNote : "Admin đã xác minh và chấp thuận khiếu nại.");
        claimRepository.save(claim);

        MerchantSettlement settlement = claim.getSettlement();
        settlement.setAdjustmentAmount(adj);
        BigDecimal baseNet = settlement.getTotalGrossRevenue()
                .subtract(settlement.getTotalDiscount())
                .subtract(settlement.getTotalCommissionFee());
        if (baseNet.compareTo(BigDecimal.ZERO) < 0) baseNet = BigDecimal.ZERO;
        settlement.setNetRevenue(baseNet.add(adj));
        settlement.setStatus(SettlementStatus.CONFIRMED);
        settlement.setConfirmedAt(LocalDateTime.now());
        settlementRepository.save(settlement);

        log.info("Admin đã giải quyết khiếu nại ID={} thành công (RESOLVED). Điều chỉnh: {}, Settlement ID={} chuyển sang CONFIRMED",
                claimId, adj, settlement.getId());

        return getAdminSettlements(null, settlement.getMerchant().getMerchantEmail()).stream()
                .filter(dto -> dto.getSettlementId().equals(settlement.getId()))
                .findFirst()
                .orElse(null);
    }

    @Override
    @Transactional
    public vn.codegyme.meal_choice.dto.settlement.AdminSettlementItemDTO rejectClaim(Long claimId, String adminNote) {
        SettlementClaim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khiếu nại ID: " + claimId));

        claim.setStatus("REJECTED");
        claim.setAdminNote(adminNote != null && !adminNote.isBlank() ? adminNote : "Admin đã xem xét và từ chối khiếu nại.");
        claimRepository.save(claim);

        MerchantSettlement settlement = claim.getSettlement();
        settlement.setStatus(SettlementStatus.PENDING_CONFIRMATION);
        settlementRepository.save(settlement);

        log.info("Admin đã từ chối khiếu nại ID={} (REJECTED). Settlement ID={} chuyển về PENDING_CONFIRMATION",
                claimId, settlement.getId());

        return getAdminSettlements(null, settlement.getMerchant().getMerchantEmail()).stream()
                .filter(dto -> dto.getSettlementId().equals(settlement.getId()))
                .findFirst()
                .orElse(null);
    }

    private String formatPeriodLabel(String periodKey) {
        if (periodKey == null || periodKey.isBlank()) return "Kỳ hiện tại";
        if (periodKey.contains("-W")) {
            try {
                String[] parts = periodKey.split("-W");
                int year = Integer.parseInt(parts[0]);
                int weekNum = Integer.parseInt(parts[1]);
                LocalDate monday = LocalDate.of(year, 1, 4)
                        .with(WeekFields.ISO.weekOfWeekBasedYear(), weekNum)
                        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate sunday = monday.plusDays(6);
                return String.format("Tuần %02d/%d (%02d/%02d - %02d/%02d)",
                        weekNum, year,
                        monday.getDayOfMonth(), monday.getMonthValue(),
                        sunday.getDayOfMonth(), sunday.getMonthValue());
            } catch (Exception e) {
                return periodKey;
            }
        }
        try {
            java.time.YearMonth ym = java.time.YearMonth.parse(periodKey);
            return String.format("Tháng %02d/%d", ym.getMonthValue(), ym.getYear());
        } catch (Exception e) {
            return periodKey;
        }
    }
}
