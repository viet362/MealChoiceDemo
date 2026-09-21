package vn.codegyme.meal_choice.service;

import vn.codegyme.meal_choice.event.UserRegisteredEvent;

public interface EmailService {

    void onUserRegistered(UserRegisteredEvent event);

    void sendMerchantRegisterEmail(
            String email,
            String restaurantName
    );

    void sendActivationEmail(
            String email,
            String displayName,
            String activationLink,
            long expirationMinutes
    );

    void sendTrustedPartnerRegistrationEmail(
            String email,
            String restaurantName
    );

    void sendTrustedPartnerApprovedEmail(
            String email,
            String restaurantName
    );

    void sendTrustedPartnerRejectedEmail(
            String email,
            String restaurantName,
            String reason
    );
}