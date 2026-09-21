package vn.codegyme.meal_choice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "merchant_settlements", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"merchant_id", "period_key"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false, columnDefinition = "VARCHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private Merchant merchant;

    @Column(name = "period_key", nullable = false, length = 50, columnDefinition = "VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String periodKey;

    @Column(name = "period_type", nullable = false, length = 20)
    @Builder.Default
    private String periodType = "MONTH";

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "total_gross_revenue", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalGrossRevenue = BigDecimal.ZERO;

    @Column(name = "total_discount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalDiscount = BigDecimal.ZERO;

    @Column(name = "commission_rate", nullable = false, precision = 8, scale = 6)
    @Builder.Default
    private BigDecimal commissionRate = BigDecimal.ZERO;

    @Column(name = "total_commission_fee", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalCommissionFee = BigDecimal.ZERO;

    @Column(name = "net_revenue", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal netRevenue = BigDecimal.ZERO;

    @Column(name = "total_orders", nullable = false)
    @Builder.Default
    private Long totalOrders = 0L;

    @Column(name = "adjustment_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal adjustmentAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private SettlementStatus status = SettlementStatus.PENDING_CONFIRMATION;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
