package vn.codegyme.meal_choice.service;

import vn.codegyme.meal_choice.dto.coupon.CouponCreateRequest;
import vn.codegyme.meal_choice.dto.coupon.CouponResponse;
import vn.codegyme.meal_choice.dto.coupon.CouponUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface CouponService {

    List<CouponResponse> getCoupons(UUID merchantId);

    CouponResponse getCoupon(Long id, UUID merchantId);

    CouponResponse createCoupon(
            CouponCreateRequest request,
            UUID merchantId
    );

    CouponResponse updateCoupon(
            Long id,
            CouponUpdateRequest request,
            UUID merchantId
    );

    void deleteCoupon(Long id, UUID merchantId);
}