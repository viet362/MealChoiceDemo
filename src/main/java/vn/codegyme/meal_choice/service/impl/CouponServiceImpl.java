package vn.codegyme.meal_choice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegyme.meal_choice.dto.coupon.CouponCreateRequest;
import vn.codegyme.meal_choice.dto.coupon.CouponResponse;
import vn.codegyme.meal_choice.dto.coupon.CouponUpdateRequest;
import vn.codegyme.meal_choice.entity.Coupon;
import vn.codegyme.meal_choice.entity.DiscountType;
import vn.codegyme.meal_choice.entity.Merchant;
import vn.codegyme.meal_choice.repository.CouponRepository;
import vn.codegyme.meal_choice.repository.MerchantRepository;
import vn.codegyme.meal_choice.service.CouponService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CouponServiceImpl implements CouponService {

    private static final BigDecimal MAX_PERCENT = new BigDecimal("100");

    private final CouponRepository couponRepository;
    private final MerchantRepository merchantRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponse> getCoupons(UUID merchantId) {
        return couponRepository
                .findAllByMerchant_IdOrderByCreatedAtDesc(merchantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponse getCoupon(Long id, UUID merchantId) {
        return toResponse(findCoupon(id, merchantId));
    }

    @Override
    public CouponResponse createCoupon(
            CouponCreateRequest request,
            UUID merchantId
    ) {
        validateCoupon(
                request.getDiscountType(),
                request.getDiscountValue(),
                request.getStartAt(),
                request.getEndAt()
        );

        String couponCode = normalizeCode(request.getCouponCode());

        if (couponRepository.existsByMerchant_IdAndCouponCode(
                merchantId,
                couponCode)) {
            throw new RuntimeException("Mã coupon đã tồn tại");
        }

        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy Merchant"));

        Coupon coupon = new Coupon();
        coupon.setMerchant(merchant);
        coupon.setCouponCode(couponCode);
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setStartAt(request.getStartAt());
        coupon.setEndAt(request.getEndAt());
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setUsedCount(0);
        coupon.setIsActive(true);

        return toResponse(couponRepository.save(coupon));
    }

    @Override
    public CouponResponse updateCoupon(
            Long id,
            CouponUpdateRequest request,
            UUID merchantId
    ) {
        Coupon coupon = findCoupon(id, merchantId);

        validateCoupon(
                request.getDiscountType(),
                request.getDiscountValue(),
                request.getStartAt(),
                request.getEndAt()
        );

        String couponCode = normalizeCode(request.getCouponCode());

        if (couponRepository.existsByMerchant_IdAndCouponCodeAndIdNot(
                merchantId,
                couponCode,
                id)) {
            throw new RuntimeException("Mã coupon đã tồn tại");
        }

        if (request.getUsageLimit() != null
                && request.getUsageLimit() < coupon.getUsedCount()) {
            throw new RuntimeException(
                    "Số lượt sử dụng không thể nhỏ hơn số lượt đã dùng"
            );
        }

        coupon.setCouponCode(couponCode);
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setStartAt(request.getStartAt());
        coupon.setEndAt(request.getEndAt());
        coupon.setUsageLimit(request.getUsageLimit());

        if (request.getIsActive() != null) {
            coupon.setIsActive(request.getIsActive());
        }

        return toResponse(couponRepository.save(coupon));
    }

    @Override
    public void deleteCoupon(Long id, UUID merchantId) {
        couponRepository.delete(findCoupon(id, merchantId));
    }

    private Coupon findCoupon(Long id, UUID merchantId) {
        return couponRepository.findByIdAndMerchant_Id(id, merchantId)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy coupon"));
    }

    private void validateCoupon(
            DiscountType discountType,
            BigDecimal discountValue,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Giá trị giảm giá phải lớn hơn 0");
        }

        if (discountType == null) {
            throw new RuntimeException("Vui lòng chọn loại giảm giá");
        }

        if (discountType == DiscountType.PERCENT
                && discountValue.compareTo(MAX_PERCENT) > 0) {
            throw new RuntimeException(
                    "Giảm giá theo phần trăm không được vượt quá 100%"
            );
        }

        if (startAt != null && endAt != null
                && !startAt.isBefore(endAt)) {
            throw new RuntimeException(
                    "Thời gian bắt đầu phải trước thời gian kết thúc"
            );
        }
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    private CouponResponse toResponse(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .couponCode(coupon.getCouponCode())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .startAt(coupon.getStartAt())
                .endAt(coupon.getEndAt())
                .usageLimit(coupon.getUsageLimit())
                .usedCount(coupon.getUsedCount())
                .isActive(coupon.getIsActive())
                .build();
    }
}