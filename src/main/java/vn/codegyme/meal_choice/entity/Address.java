package vn.codegyme.meal_choice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = { "user" })
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // ========== THÔNG TIN LIÊN HỆ ==========

    @Column(nullable = false, length = 100)
    private String contactName; // Tên người nhận (*)

    @Column(nullable = false, length = 20)
    private String contactPhone; // Số điện thoại người nhận (*)

    // ========== ĐỊA CHỈ ==========

    @Column(nullable = false, length = 100)
    private String city; // Tỉnh/Thành phố (*)

    @Column(nullable = false, length = 100)
    private String district; // Quận/Huyện (*)

    @Column(nullable = false, length = 100)
    private String ward; // Phường/Xã (*)

    @Column(nullable = false, length = 255)
    private String street; // Tên đường, Tòa nhà, Số nhà (*)

    @Column(length = 500)
    private String note; // Ghi chú thêm

    //Tọa độ để tính khoảng cách
    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Builder.Default
    @Column(nullable = false)
    @ColumnDefault("false") // Sinh ra DDL: DEFAULT false
    private Boolean isDefault = false; // Địa chỉ mặc định

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ================= CONVENIENCE CONSTRUCTORS =================

    public Address(String contactName, String contactPhone, String city,
                   String district, String ward, String street) {
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.city = city;
        this.district = district;
        this.ward = ward;
        this.street = street;
        this.isDefault = false;
    }

    // ================= HELPER METHODS =================

    /**
     * Lấy địa chỉ đầy đủ dạng text
     */
    public String getFullAddress() {
        return String.format("%s, %s, %s, %s", street, ward, district, city);
    }

    /**
     * Lấy thông tin liên hệ đầy đủ
     */
    public String getContactInfo() {
        return String.format("%s - %s", contactName, contactPhone);
    }
}