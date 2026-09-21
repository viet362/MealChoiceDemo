package vn.codegyme.meal_choice.service;


import java.math.BigDecimal;
import java.util.UUID;

public record ShippingQuote(

        UUID partnerId,

        String partnerName,

        double distanceKm,

        BigDecimal shippingFee,

        boolean peakHour

) {
}
