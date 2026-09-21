package vn.codegyme.meal_choice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "merchant_addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MerchantAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "merchant_id",
            nullable = false
    )
    private Merchant merchant;

    @Column(
            name = "merchant_address",
            nullable = false,
            length = 255
    )
    private String merchantAddress;

    // Địa chỉ hành chính mới
    @Column(
            name = "province_code",
            nullable = false,
            length = 20
    )
    private String provinceCode;

    @Column(
            name = "district_code",
            nullable = false,
            length = 20
    )
    private String districtCode;

    @Column(
            name = "ward_code",
            nullable = false,
            length = 20
    )
    private String wardCode;

    @Column(name = "merchant_open_time")
    private LocalTime merchantOpenTime;

    @Column(name = "merchant_close_time")
    private LocalTime merchantCloseTime;

    @Column(
            name = "is_default",
            nullable = false
    )
    private boolean isDefault = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    //Tọa độ để tính khoảng cach
    @Column (name = "latitude")
    private Double latitude;

    @Column (name = "longitude")
    private Double longitude;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }

        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}