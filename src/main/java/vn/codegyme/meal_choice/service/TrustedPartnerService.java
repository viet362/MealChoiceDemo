package vn.codegyme.meal_choice.service;

import java.util.UUID;

public interface TrustedPartnerService {

    /**
     * Merchant đăng ký làm đối tác thân thiết.
     * Điều kiện: doanh thu tháng > 100.000.000 VNĐ.
     */
    void registerTrustedPartner(UUID merchantId);
}