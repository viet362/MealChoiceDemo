-- Active: 1785914295113@@mysql-22176-buivietbacn01-1ff7.a.aivencloud.com@10055@defaultdb
-- =============================================================================
-- MEALCHOICE DATABASE SCHEMA
-- Hệ Quản Trị Cơ Sở Dữ Liệu: MySQL 8.0+ / Aiven Cloud MySQL
-- =============================================================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS settlement_claims;

DROP TABLE IF EXISTS merchant_settlements;

DROP TABLE IF EXISTS food_vouchers;

DROP TABLE IF EXISTS order_items;

DROP TABLE IF EXISTS orders;

DROP TABLE IF EXISTS delivery_partners;

DROP TABLE IF EXISTS vouchers;

DROP TABLE IF EXISTS merchant_likes;

DROP TABLE IF EXISTS food_likes;

DROP TABLE IF EXISTS food_images;

DROP TABLE IF EXISTS food_tag_mapping;

DROP TABLE IF EXISTS food_category_mapping;

DROP TABLE IF EXISTS foods;

DROP TABLE IF EXISTS tags;

DROP TABLE IF EXISTS food_categories;

DROP TABLE IF EXISTS merchant_addresses;

DROP TABLE IF EXISTS merchants;

DROP TABLE IF EXISTS addresses;

DROP TABLE IF EXISTS refresh_tokens;

DROP TABLE IF EXISTS activation_tokens;

DROP TABLE IF EXISTS user_roles;

DROP TABLE IF EXISTS users;

DROP TABLE IF EXISTS roles;

DROP TABLE IF EXISTS cart_items;

DROP TABLE IF EXISTS carts;

DROP TABLE IF EXISTS merchant_payout_requests;

DROP TABLE IF EXISTS trusted_partner_requests;

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- 1. BẢNG ROLES (Vai trò người dùng)
-- =============================================================================
CREATE TABLE roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) UNIQUE NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- 2. BẢNG USERS (Người dùng hệ thống)
-- =============================================================================
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(60) NOT NULL,
    display_name VARCHAR(32) NOT NULL,
    phone_number VARCHAR(20) UNIQUE NOT NULL,
    gender ENUM('FEMALE', 'MALE', 'OTHER') NULL,
    avatar_url VARCHAR(255) NULL,
    dob DATE NULL,
    latitude DOUBLE NULL,
    longitude DOUBLE NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 3. BẢNG USER_ROLES (Phân quyền người dùng)
-- =============================================================================
CREATE TABLE user_roles (
    user_id VARCHAR(36) NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 4. BẢNG ACTIVATION_TOKENS (Token kích hoạt tài khoản qua email)
-- =============================================================================
CREATE TABLE activation_tokens (
    id VARCHAR(36) PRIMARY KEY,
    token VARCHAR(36) UNIQUE NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    expiry_date DATETIME(6) NOT NULL,
    CONSTRAINT fk_activation_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 5. BẢNG REFRESH_TOKENS (Token làm mới phiên đăng nhập JWT)
-- =============================================================================
CREATE TABLE refresh_tokens (
    id VARCHAR(36) PRIMARY KEY,
    token VARCHAR(500) UNIQUE NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    expiry_date DATETIME(6) NOT NULL,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 6. BẢNG ADDRESSES (Sổ địa chỉ nhận hàng của User)
-- =============================================================================
CREATE TABLE addresses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    contact_name VARCHAR(100) NOT NULL,
    contact_phone VARCHAR(20) NOT NULL,
    city VARCHAR(100) NOT NULL,
    district VARCHAR(100) NOT NULL,
    ward VARCHAR(100) NOT NULL,
    street VARCHAR(255) NOT NULL,
    note VARCHAR(500) NULL,
    latitude DOUBLE NULL,
    longitude DOUBLE NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    user_id VARCHAR(36) NOT NULL,
    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 7. BẢNG MERCHANTS (Thông tin Cửa hàng / Đối tác Merchant)
-- =============================================================================
CREATE TABLE merchants (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) UNIQUE NULL,
    merchant_restaurant_name VARCHAR(150) NOT NULL,
    merchant_email VARCHAR(255) UNIQUE NOT NULL,
    merchant_phone VARCHAR(20) UNIQUE NOT NULL,
    merchant_status VARCHAR(30) DEFAULT 'PENDING',
    lock_reason VARCHAR(500) NULL,
    locked_at DATETIME(6) NULL,
    reject_reason VARCHAR(500) NULL,
    rejected_at DATETIME(6) NULL,
    is_trusted_partner BOOLEAN NOT NULL DEFAULT FALSE,
    bank_name VARCHAR(100) NULL,
    bank_account_number VARCHAR(50) NULL,
    CONSTRAINT fk_merchants_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 8. BẢNG MERCHANT_ADDRESSES (Chi nhánh / Địa chỉ của Cửa hàng)
-- =============================================================================
CREATE TABLE merchant_addresses (
    id VARCHAR(36) PRIMARY KEY,
    merchant_id VARCHAR(36) NOT NULL,
    merchant_address VARCHAR(255) NOT NULL,
    province_code VARCHAR(20) NOT NULL,
    district_code VARCHAR(20) NOT NULL,
    ward_code VARCHAR(20) NOT NULL,
    merchant_open_time TIME NULL,
    merchant_close_time TIME NULL,
    latitude DOUBLE NULL,
    longitude DOUBLE NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_merchant_addresses_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 9. BẢNG FOOD_CATEGORIES (Danh mục món ăn)
-- =============================================================================
CREATE TABLE food_categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_name VARCHAR(100) NOT NULL UNIQUE,
    category_description VARCHAR(255) NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 10. BẢNG TAGS (Thẻ phân loại / Nhãn món ăn)
-- =============================================================================
CREATE TABLE tags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tag_name VARCHAR(100) NOT NULL UNIQUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 11. BẢNG FOODS (Món ăn / Sản phẩm)
-- =============================================================================
CREATE TABLE foods (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id VARCHAR(36) NOT NULL,
    merchant_address_id VARCHAR(36) NOT NULL,
    food_name VARCHAR(150) NOT NULL,
    preparation_time INT NULL,
    food_note TEXT NULL,
    price DECIMAL(12, 2) NOT NULL,
    discount_price DECIMAL(12, 2) NULL,
    service_fee DECIMAL(12, 2) DEFAULT 0,
    views INT NOT NULL DEFAULT 0,
    order_count INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_recommended BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,
    CONSTRAINT fk_foods_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id) ON DELETE CASCADE,
    CONSTRAINT fk_foods_merchant_address FOREIGN KEY (merchant_address_id) REFERENCES merchant_addresses (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 12. BẢNG FOOD_CATEGORY_MAPPING (Quan hệ nhiều - nhiều Món ăn & Danh mục)
-- =============================================================================
CREATE TABLE food_category_mapping (
    food_id BIGINT NOT NULL,
    food_category_id BIGINT NOT NULL,
    PRIMARY KEY (food_id, food_category_id),
    CONSTRAINT fk_fcm_food FOREIGN KEY (food_id) REFERENCES foods (id) ON DELETE CASCADE,
    CONSTRAINT fk_fcm_category FOREIGN KEY (food_category_id) REFERENCES food_categories (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 13. BẢNG FOOD_TAG_MAPPING (Quan hệ nhiều - nhiều Món ăn & Thẻ Tag)
-- =============================================================================
CREATE TABLE food_tag_mapping (
    food_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (food_id, tag_id),
    CONSTRAINT fk_ftm_food FOREIGN KEY (food_id) REFERENCES foods (id) ON DELETE CASCADE,
    CONSTRAINT fk_ftm_tag FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 14. BẢNG FOOD_IMAGES (Bộ sưu tập hình ảnh món ăn)
-- =============================================================================
CREATE TABLE food_images (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    food_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_food_images_food FOREIGN KEY (food_id) REFERENCES foods (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 15. BẢNG FOOD_LIKES (Người dùng yêu thích món ăn)
-- =============================================================================
CREATE TABLE food_likes (
    user_id VARCHAR(36) NOT NULL,
    food_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, food_id),
    CONSTRAINT fk_food_likes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_food_likes_food FOREIGN KEY (food_id) REFERENCES foods (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 16. BẢNG MERCHANT_LIKES (Người dùng yêu thích quán ăn)
-- =============================================================================
CREATE TABLE merchant_likes (
    user_id VARCHAR(36) NOT NULL,
    merchant_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (user_id, merchant_id),
    CONSTRAINT fk_merchant_likes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_merchant_likes_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 17. BẢNG VOUCHERS (Mã giảm giá khuyến mãi)
-- =============================================================================
CREATE TABLE vouchers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id VARCHAR(36) NOT NULL,
    voucher_code VARCHAR(50) NOT NULL,
    discount_type ENUM('PERCENT', 'FIXED') NOT NULL,
    discount_value DECIMAL(12, 2) NOT NULL,
    start_at DATETIME NULL,
    end_at DATETIME NULL,
    usage_limit INT NULL,
    used_count INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_vouchers_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id) ON DELETE CASCADE,
    UNIQUE (merchant_id, voucher_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 18. BẢNG DELIVERY_PARTNERS (Đối tác vận chuyển)
-- =============================================================================
CREATE TABLE delivery_partners (
    id VARCHAR(36) PRIMARY KEY,
    partner_code VARCHAR(50) UNIQUE NOT NULL,
    partner_name VARCHAR(150) NOT NULL,
    email VARCHAR(100) NULL,
    phone VARCHAR(20) NULL,
    address VARCHAR(255) NULL,
    logo_url VARCHAR(255) NULL,
    base_fee DECIMAL(12, 2) NOT NULL,
    base_distance_km DOUBLE NOT NULL,
    fee_per_km DECIMAL(12, 2) NOT NULL,
    peak_multiplier DECIMAL(5, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    lock_reason VARCHAR(500) NULL,
    locked_at DATETIME(6) NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 19. BẢNG ORDERS (Đơn đặt hàng)
-- =============================================================================
CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_code VARCHAR(50) UNIQUE NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    merchant_id VARCHAR(36) NOT NULL,
    delivery_partner_id VARCHAR(36) NULL,
    contact_name VARCHAR(100) NOT NULL,
    contact_phone VARCHAR(20) NOT NULL,
    delivery_address VARCHAR(500) NOT NULL,
    note TEXT NULL,
    status ENUM('CANCELLED', 'COMPLETED', 'DELIVERING', 'PENDING', 'PREPARING') NOT NULL DEFAULT 'PENDING',
    payment_method ENUM('CARD', 'COD') NOT NULL DEFAULT 'COD',
    subtotal_price DECIMAL(12, 2) NOT NULL,
    shipping_fee DECIMAL(12, 2) NOT NULL DEFAULT 0,
    service_fee DECIMAL(12, 2) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(12, 2) NOT NULL,
    cancel_reason VARCHAR(500) NULL,
    estimated_delivery_time DATETIME NULL,
    completed_at DATETIME(6) NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_orders_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id),
    CONSTRAINT fk_orders_delivery_partner FOREIGN KEY (delivery_partner_id) REFERENCES delivery_partners (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 20. BẢNG ORDER_ITEMS (Chi tiết từng món ăn trong đơn hàng)
-- =============================================================================
CREATE TABLE order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    food_id BIGINT NOT NULL,
    food_name VARCHAR(150) NOT NULL,
    food_image VARCHAR(500) NULL,
    price DECIMAL(12, 2) NOT NULL,
    quantity INT NOT NULL,
    subtotal DECIMAL(12, 2) NOT NULL,
    note VARCHAR(255) NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_food FOREIGN KEY (food_id) REFERENCES foods (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 21. BẢNG food_coupons (map bảng food với coupon)
-- =============================================================================
CREATE TABLE food_vouchers (
                               food_id BIGINT NOT NULL,
                               voucher_id BIGINT NOT NULL,
                               PRIMARY KEY (food_id, voucher_id),
                               CONSTRAINT fk_food_vouchers_food
                                   FOREIGN KEY (food_id) REFERENCES foods(id)
                                       ON DELETE CASCADE,
                               CONSTRAINT fk_food_vouchers_voucher
                                   FOREIGN KEY (voucher_id) REFERENCES vouchers(id)
                                       ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- 22. BẢNG MERCHANT_PAYOUT_REQUESTS (Yêu cầu rút tiền / thanh lý của Merchant)
-- =============================================================================
CREATE TABLE IF NOT EXISTS merchant_payout_requests (
    id VARCHAR(36) PRIMARY KEY,
    merchant_id VARCHAR(36) NOT NULL,
    type VARCHAR(30) NOT NULL,
    amount DECIMAL(18, 0) NOT NULL,
    bank_name VARCHAR(100) NOT NULL,
    bank_account_number VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    admin_note VARCHAR(1000) NULL,
    transfer_proof_url VARCHAR(500) NULL,
    created_at DATETIME(6) NULL,
    completed_at DATETIME(6) NULL,
    rejected_at DATETIME(6) NULL,
    CONSTRAINT fk_payout_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 23. BẢNG MERCHANT_SETTLEMENTS (Kỳ đối soát doanh thu & quyết toán của Merchant)
-- =============================================================================
CREATE TABLE IF NOT EXISTS merchant_settlements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id VARCHAR(36) NOT NULL,
    period_key VARCHAR(50) NOT NULL,
    period_type VARCHAR(20) NOT NULL DEFAULT 'MONTH',
    start_date DATETIME NOT NULL,
    end_date DATETIME NOT NULL,
    total_gross_revenue DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    total_discount DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    commission_rate DECIMAL(8, 6) NOT NULL DEFAULT 0.000000,
    total_commission_fee DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    net_revenue DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    total_orders BIGINT NOT NULL DEFAULT 0,
    adjustment_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_CONFIRMATION',
    confirmed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_merchant_period UNIQUE (merchant_id, period_key),
    CONSTRAINT fk_settlements_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 24. BẢNG SETTLEMENT_CLAIMS (Khiếu nại đối soát doanh thu của Merchant)
-- =============================================================================
CREATE TABLE IF NOT EXISTS settlement_claims (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    settlement_id BIGINT NOT NULL,
    merchant_id VARCHAR(36) NOT NULL,
    reason VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    evidence_image_url VARCHAR(500) NULL,
    adjustment_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    admin_note TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_claims_settlement FOREIGN KEY (settlement_id) REFERENCES merchant_settlements (id) ON DELETE CASCADE,
    CONSTRAINT fk_claims_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
 
-- =============================================================================
-- 25. BẢNG TRUSTED_PARTNER_REQUESTS (Yêu cầu đăng ký Đối tác thân thiết của Merchant)
-- =============================================================================
CREATE TABLE IF NOT EXISTS trusted_partner_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id VARCHAR(36) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    revenue DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    reject_reason VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at DATETIME NULL,
    CONSTRAINT fk_trusted_partner_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

