package vn.codegyme.meal_choice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Giỏ hàng của một User.
 *
 * Mỗi User chỉ có duy nhất một giỏ hàng và giỏ hàng chỉ chứa món của
 * MỘT Merchant tại một thời điểm (giống Shopee Food / GrabFood).
 *
 * LƯU Ý QUAN TRỌNG: Giỏ hàng KHÔNG lưu giá món.
 * Giá luôn được đọc trực tiếp từ bảng foods khi hiển thị và khi đặt hàng,
 * nên người dùng không bao giờ nhìn thấy giá cũ đã bị merchant thay đổi.
 */
@Entity
@Table(name = "carts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true,
            columnDefinition = "VARCHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
    )
    private User user;

    /**
     * Merchant mà giỏ hàng đang gắn với.
     * NULL khi giỏ hàng rỗng.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "merchant_id",
            columnDefinition = "VARCHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
    )
    private Merchant merchant;

    @OneToMany(
            mappedBy = "cart",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ================= HELPER METHODS =================

    public void addItem(CartItem item) {
        items.add(item);
        item.setCart(this);
    }

    public void removeItem(CartItem item) {
        items.remove(item);
        item.setCart(null);
    }

    /**
     * Xóa sạch món trong giỏ và gỡ liên kết Merchant.
     */
    public void clear() {
        items.clear();
        merchant = null;
    }

    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }
}