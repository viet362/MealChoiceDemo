package vn.codegyme.meal_choice.service;

import java.util.UUID;

public interface AdminPayoutService {

        void completePayoutRequest(
                        UUID requestId,
                        String transferProofUrl,
                        String adminNote);

        void rejectPayoutRequest(
                        UUID requestId,
                        String reason);
}
