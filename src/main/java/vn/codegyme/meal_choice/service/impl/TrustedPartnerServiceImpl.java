package vn.codegyme.meal_choice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegyme.meal_choice.entity.Merchant;
import vn.codegyme.meal_choice.entity.OrderStatus;
import vn.codegyme.meal_choice.entity.TrustedPartnerRequest;
import vn.codegyme.meal_choice.entity.TrustedPartnerRequestStatus;
import vn.codegyme.meal_choice.repository.MerchantRepository;
import vn.codegyme.meal_choice.repository.OrderRepository;
import vn.codegyme.meal_choice.repository.TrustedPartnerRequestRepository;
import vn.codegyme.meal_choice.service.EmailService;
import vn.codegyme.meal_choice.service.TrustedPartnerService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrustedPartnerServiceImpl implements TrustedPartnerService {

    private static final BigDecimal MIN_REVENUE =
            new BigDecimal("100000000");

    private final MerchantRepository merchantRepository;
    private final OrderRepository orderRepository;
    private final TrustedPartnerRequestRepository trustedPartnerRequestRepository;
    private final EmailService emailService;

    @Override
    @Transactional
    public void registerTrustedPartner(UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy Merchant"));

        if (merchant.isTrustedPartner()) {
            throw new RuntimeException(
                    "Merchant đã là đối tác thân thiết");
        }

        if (trustedPartnerRequestRepository.existsByMerchant_IdAndStatus(
                merchantId,
                TrustedPartnerRequestStatus.PENDING)) {

            throw new RuntimeException(
                    "Merchant đã đăng ký và đang chờ Admin duyệt");
        }

        YearMonth currentMonth = YearMonth.now();

        LocalDateTime startDate = currentMonth
                .atDay(1)
                .atStartOfDay();

        LocalDateTime endDate = currentMonth
                .plusMonths(1)
                .atDay(1)
                .atStartOfDay();

        BigDecimal revenue = orderRepository.calculateRevenue(
                merchantId,
                OrderStatus.COMPLETED,
                startDate,
                endDate
        );

        if (revenue.compareTo(MIN_REVENUE) < 0) {
            throw new RuntimeException(
                    "Doanh thu tháng chưa đạt 100.000.000 VNĐ");
        }

        TrustedPartnerRequest request = new TrustedPartnerRequest();
        request.setMerchant(merchant);
        request.setStatus(TrustedPartnerRequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());
        request.setRevenue(revenue);

        trustedPartnerRequestRepository.save(request);

        emailService.sendTrustedPartnerRegistrationEmail(
                merchant.getMerchantEmail(),
                merchant.getMerchantRestaurantName()
        );
    }
}