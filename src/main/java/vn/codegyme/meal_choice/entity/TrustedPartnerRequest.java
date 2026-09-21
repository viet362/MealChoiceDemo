package vn.codegyme.meal_choice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trusted_partner_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TrustedPartnerRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "merchant_id",
            nullable = false,
            columnDefinition = "VARCHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
    )
    private Merchant merchant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TrustedPartnerRequestStatus status = TrustedPartnerRequestStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal revenue;
}