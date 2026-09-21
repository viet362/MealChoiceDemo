package vn.codegyme.meal_choice.service;

import org.springframework.stereotype.Service;
import vn.codegyme.meal_choice.entity.DeliveryPartner;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
public class ShippingFeeService {

    private static final ZoneId VIETNAM_ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");


    /**
     * Kiểm tra hiện tại có phải giờ cao điểm không.
     */
    public boolean isPeakHour() {

        LocalTime now = ZonedDateTime
                .now(VIETNAM_ZONE)
                .toLocalTime();


        // Cao điểm buổi trưa
        LocalTime lunchStart = LocalTime.of(11, 0);
        LocalTime lunchEnd = LocalTime.of(13, 30);


        // Cao điểm buổi tối
        LocalTime dinnerStart = LocalTime.of(17, 30);
        LocalTime dinnerEnd = LocalTime.of(20, 0);


        boolean lunchPeak =
                !now.isBefore(lunchStart)
                        && now.isBefore(lunchEnd);


        boolean dinnerPeak =
                !now.isBefore(dinnerStart)
                        && now.isBefore(dinnerEnd);


        return lunchPeak || dinnerPeak;
    }
    public BigDecimal calculateShippingFee(
            DeliveryPartner partner,
            double distanceKm
    ) {

        BigDecimal fee = partner.getBaseFee();

        // Tính phần km vượt quá
        if (distanceKm > partner.getBaseDistanceKm()) {

            double extraKm =
                    distanceKm - partner.getBaseDistanceKm();

            fee = fee.add(
                    partner.getFeePerKm()
                            .multiply(
                                    BigDecimal.valueOf(extraKm)
                            )
            );
        }


        // Nếu giờ cao điểm -> nhân hệ số
        if (isPeakHour()) {

            fee = fee.multiply(
                    partner.getPeakMultiplier()
            );
        }


        return fee.setScale(
                0,
                RoundingMode.HALF_UP
        );
    }
}
