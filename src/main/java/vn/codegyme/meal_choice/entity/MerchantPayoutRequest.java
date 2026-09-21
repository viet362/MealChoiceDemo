package vn.codegyme.meal_choice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "merchant_payout_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantPayoutRequest {

        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        @JdbcTypeCode(SqlTypes.VARCHAR)
        @Column(length = 36)
        private UUID id;

        // ==========================================
        // MERCHANT
        // ==========================================

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "merchant_id", nullable = false, columnDefinition = "VARCHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
        private Merchant merchant;

        // ==========================================
        // LOẠI YÊU CẦU
        // ==========================================

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 30)
        private PayoutRequestType type;

        // ==========================================
        // SỐ TIỀN
        // ==========================================

        @Column(nullable = false, precision = 18, scale = 0)
        private BigDecimal amount;

        // ==========================================
        // SNAPSHOT TÀI KHOẢN NGÂN HÀNG
        // ==========================================

        @Column(name = "bank_name", nullable = false, length = 100)
        private String bankName;

        @Column(name = "bank_account_number", nullable = false, length = 50)
        private String bankAccountNumber;

        // ==========================================
        // TRẠNG THÁI YÊU CẦU
        // ==========================================

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 30)
        private PayoutRequestStatus status;

        // ==========================================
        // PHẢN HỒI ADMIN
        // ==========================================

        @Column(length = 1000)
        private String adminNote;

        @Column(length = 500)
        private String transferProofUrl;

        // ==========================================
        // THỜI GIAN
        // ==========================================

        private LocalDateTime createdAt;

        private LocalDateTime completedAt;

        private LocalDateTime rejectedAt;

        @PrePersist
        public void onCreate() {

                createdAt = LocalDateTime.now();

                if (status == null) {
                        status = PayoutRequestStatus.PENDING;
                }
        }
}
