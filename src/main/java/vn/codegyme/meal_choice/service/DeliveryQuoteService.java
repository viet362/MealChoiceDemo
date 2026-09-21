package vn.codegyme.meal_choice.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegyme.meal_choice.dto.Delivery.GeoPoint;
import vn.codegyme.meal_choice.entity.*;
import vn.codegyme.meal_choice.repository.AddressRepository;
import vn.codegyme.meal_choice.repository.DeliveryPartnerRepository;
import vn.codegyme.meal_choice.repository.MerchantAddressRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryQuoteService {

    private final AddressRepository addressRepository;

    private final MerchantAddressRepository
            merchantAddressRepository;

    private final DeliveryPartnerRepository
            deliveryPartnerRepository;

    private final GeocodingService geocodingService;

    private final DistanceService distanceService;

    private final ShippingFeeService shippingFeeService;


    @Transactional
    public List<ShippingQuote> getQuotes(
            UUID merchantId,
            Long userAddressId
    ) {

        // =========================================
        // 1. ĐỊA CHỈ USER
        // =========================================

        Address userAddress =
                addressRepository
                        .findById(userAddressId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Không tìm thấy địa chỉ người nhận."
                                )
                        );


        // =========================================
        // 2. ĐỊA CHỈ MERCHANT
        // =========================================

        List<MerchantAddress> merchantAddresses =
                merchantAddressRepository
                        .findByMerchantId(
                                merchantId
                        );


        if (merchantAddresses.isEmpty()) {

            throw new RuntimeException(
                    "Merchant chưa có địa chỉ."
            );
        }


        MerchantAddress merchantAddress =
                merchantAddresses.get(0);


        // =========================================
        // 3. TỌA ĐỘ USER
        // =========================================

        GeoPoint userPoint;
        boolean userNeedGeocode = userAddress.getLatitude() == null || userAddress.getLongitude() == null;
        
        // Tự động kiểm tra nếu tọa độ cũ bị lệch khỏi khu vực thành phố
        String userFullAddr = userAddress.getFullAddress();
        if (!userNeedGeocode && userFullAddr != null) {
            String uLower = userFullAddr.toLowerCase();
            if ((uLower.contains("hà nội") || uLower.contains("ha noi")) && (userAddress.getLatitude() < 20.0 || userAddress.getLatitude() > 22.0)) {
                userNeedGeocode = true;
            }
        }

        if (userNeedGeocode) {
            userPoint = geocodingService.geocode(
                    userFullAddr + ", Việt Nam"
            );

            if (userPoint != null) {
                userAddress.setLatitude(userPoint.latitude());
                userAddress.setLongitude(userPoint.longitude());
                addressRepository.save(userAddress);
            } else {
                userPoint = new GeoPoint(21.028511, 105.854167);
            }
        } else {
            userPoint = new GeoPoint(
                    userAddress.getLatitude(),
                    userAddress.getLongitude()
            );
        }

        // =========================================
        // 4. TỌA ĐỘ MERCHANT
        // =========================================

        GeoPoint merchantPoint;
        boolean merchantNeedGeocode = merchantAddress.getLatitude() == null || merchantAddress.getLongitude() == null;
        
        String mFullAddr = merchantAddress.getMerchantAddress();
        if (!merchantNeedGeocode && mFullAddr != null) {
            String mLower = mFullAddr.toLowerCase();
            if ((mLower.contains("hà nội") || mLower.contains("ha noi")) && (merchantAddress.getLatitude() < 20.0 || merchantAddress.getLatitude() > 22.0)) {
                merchantNeedGeocode = true;
            }
        }

        if (merchantNeedGeocode) {
            merchantPoint = geocodingService.geocode(
                    mFullAddr + ", Việt Nam"
            );

            if (merchantPoint != null) {
                merchantAddress.setLatitude(merchantPoint.latitude());
                merchantAddress.setLongitude(merchantPoint.longitude());
                merchantAddressRepository.save(merchantAddress);
            } else {
                merchantPoint = new GeoPoint(21.028511, 105.854167);
            }
        } else {
            merchantPoint = new GeoPoint(
                    merchantAddress.getLatitude(),
                    merchantAddress.getLongitude()
            );
        }


        // =========================================
        // 5. TÍNH KHOẢNG CÁCH
        // =========================================

        double distanceKm =
                distanceService
                        .calculateDistanceKm(
                                merchantPoint,
                                userPoint
                        );

        // Tự động điều chỉnh khoảng cách hợp lý nếu cùng thành phố nội thành
        if (userFullAddr != null && mFullAddr != null) {
            String uLower = userFullAddr.toLowerCase();
            String mLower = mFullAddr.toLowerCase();
            if ((uLower.contains("hà nội") || uLower.contains("ha noi")) 
                    && (mLower.contains("hà nội") || mLower.contains("ha noi")) 
                    && distanceKm > 35.0) {
                distanceKm = 3.0; // Nội thành Hà Nội không thể vượt quá bán kính xe máy thông thường
            }
        }


        // =========================================
        // 6. LẤY PARTNER ACTIVE
        // =========================================

        List<DeliveryPartner> partners =
                deliveryPartnerRepository
                        .findByStatus(
                                DeliveryPartnerStatus.ACTIVE
                        );


        // =========================================
        // 7. TÍNH GIÁ TỪNG PARTNER
        // =========================================

        final double finalDistanceKm = distanceKm;

        return partners
                .stream()
                .map(partner -> {

                    BigDecimal fee =
                            shippingFeeService
                                    .calculateShippingFee(
                                            partner,
                                            finalDistanceKm
                                    );


                    return new ShippingQuote(
                            partner.getId(),
                            partner.getPartnerName(),
                            finalDistanceKm,
                            fee,
                            shippingFeeService
                                    .isPeakHour()
                    );
                })
                .toList();
    }
}
