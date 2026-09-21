package vn.codegyme.meal_choice.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "delivery_partners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryPartner {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String partnerCode;

    @Column(nullable = false, length = 150)
    private String partnerName;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String address;

    private String logoUrl;


    // =============================
    // CẤU HÌNH GIÁ
    // =============================

    // Phí cơ bản, VD 15.000đ
    @Column(nullable = false)
    private BigDecimal baseFee;

    // Số km nằm trong phí cơ bản, VD 3km
    @Column(nullable = false)
    private Double baseDistanceKm;

    // Giá mỗi km tiếp theo
    @Column(nullable = false)
    private BigDecimal feePerKm;

    // Hệ số giờ cao điểm, VD 1.2
    @Column(nullable = false)
    private BigDecimal peakMultiplier;


    // =============================
    // TRẠNG THÁI
    // =============================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryPartnerStatus status;

    @Column(length = 500)
    private String lockReason;

    private LocalDateTime lockedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;


    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (status == null) {
            status = DeliveryPartnerStatus.ACTIVE;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
