package vn.codegyme.meal_choice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "merchants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(
            name = "merchant_restaurant_name",
            nullable = false,
            length = 150
    )
    private String merchantRestaurantName;

    @Column(
            name = "merchant_email",
            unique = true,
            nullable = false,
            length = 255
    )
    private String merchantEmail;

    @Column(
            name = "merchant_phone",
            unique = true,
            nullable = false,
            length = 20
    )
    private String merchantPhone;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "merchant_status",
            length = 30
    )
    private MerchantStatus merchantStatus;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_account_number", length = 50)
    private String bankAccountNumber;

    // ===== Lock / Reject =====

    @Column(name = "lock_reason", length = 500)
    private String lockReason;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    // ===== Other =====

    @Column(name = "is_trusted_partner", nullable = false)
    private boolean trustedPartner = false;

    @OneToOne
    @JoinColumn(
            name = "user_id",
            unique = true
    )
    private User user;

    @OneToMany(
            mappedBy = "merchant",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<MerchantAddress> addresses = new ArrayList<>();

    @OneToMany(
            mappedBy = "merchant",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Food> foods = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "merchant_likes",
            joinColumns = @JoinColumn(name = "merchant_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> likedByUsers = new ArrayList<>();

    @OneToMany(
            mappedBy = "merchant",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<TrustedPartnerRequest> trustedPartnerRequests = new ArrayList<>();
}