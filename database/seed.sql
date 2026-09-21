-- Active: 1785914295113@@mysql-22176-buivietbacn01-1ff7.a.aivencloud.com@10055@defaultdb
-- =============================================================================
-- MEALCHOICE DATABASE SEED DATA
-- Mật khẩu mặc định cho tất cả tài khoản là: 123456
-- (Bcrypt hash: $2a$10$62.R/1PKPrBjrT4U9zOuuuGvdNcySDhi0pk3xy/NDqEQKXwEnexPu)
-- =============================================================================

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE merchant_payout_requests;
TRUNCATE TABLE trusted_partner_requests;
TRUNCATE TABLE settlement_claims;
TRUNCATE TABLE merchant_settlements;
TRUNCATE TABLE food_vouchers;
TRUNCATE TABLE order_items;
TRUNCATE TABLE orders;
TRUNCATE TABLE delivery_partners;
TRUNCATE TABLE vouchers;
TRUNCATE TABLE merchant_likes;
TRUNCATE TABLE food_likes;
TRUNCATE TABLE food_images;
TRUNCATE TABLE food_tag_mapping;
TRUNCATE TABLE food_category_mapping;
TRUNCATE TABLE foods;
TRUNCATE TABLE tags;
TRUNCATE TABLE food_categories;
TRUNCATE TABLE merchant_addresses;
TRUNCATE TABLE merchants;
TRUNCATE TABLE addresses;
TRUNCATE TABLE refresh_tokens;
TRUNCATE TABLE activation_tokens;
TRUNCATE TABLE user_roles;
TRUNCATE TABLE users;
TRUNCATE TABLE roles;

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- 1. ROLES (Vai trò)
-- =============================================================================
INSERT INTO
    roles (id, name)
VALUES (1, 'ROLE_ADMIN'),
    (2, 'ROLE_USER'),
    (3, 'ROLE_MERCHANT');

-- =============================================================================
-- 2. USERS (Người dùng)
-- =============================================================================
INSERT INTO
    users (
        id,
        email,
        password,
        display_name,
        phone_number,
        gender,
        avatar_url,
        dob,
        is_active,
        created_at
    )
VALUES (
        '00000000-0000-0000-0000-000000000001',
        'admin@gmail.com',
        '$2a$10$62.R/1PKPrBjrT4U9zOuuuGvdNcySDhi0pk3xy/NDqEQKXwEnexPu',
        'Quản Trị Viên',
        '0900000001',
        'MALE',
        'https://api.dicebear.com/7.x/bottts/svg?seed=Admin',
        '1995-01-01',
        TRUE,
        NOW()
    ),
    (
        '00000000-0000-0000-0000-000000000002',
        'anh2k3le@gmail.com',
        '$2a$10$62.R/1PKPrBjrT4U9zOuuuGvdNcySDhi0pk3xy/NDqEQKXwEnexPu',
        'Nguyễn Văn Anh',
        '0900000002',
        'MALE',
        'https://api.dicebear.com/7.x/avataaars/svg?seed=Anh',
        '2000-05-10',
        TRUE,
        NOW()
    ),
    (
        '00000000-0000-0000-0000-000000000003',
        'user2@gmail.com',
        '$2a$10$62.R/1PKPrBjrT4U9zOuuuGvdNcySDhi0pk3xy/NDqEQKXwEnexPu',
        'Trần Thị Bình',
        '0900000003',
        'FEMALE',
        'https://api.dicebear.com/7.x/avataaars/svg?seed=Binh',
        '1999-11-20',
        TRUE,
        NOW()
    ),
    (
        '00000000-0000-0000-0000-000000000004',
        'merchant1@gmail.com',
        '$2a$10$62.R/1PKPrBjrT4U9zOuuuGvdNcySDhi0pk3xy/NDqEQKXwEnexPu',
        'Chủ Quán Bún Chả',
        '0900000004',
        'MALE',
        NULL,
        '1988-08-15',
        TRUE,
        NOW()
    ),
    (
        '00000000-0000-0000-0000-000000000005',
        'merchant2@gmail.com',
        '$2a$10$62.R/1PKPrBjrT4U9zOuuuGvdNcySDhi0pk3xy/NDqEQKXwEnexPu',
        'Chủ Quán Trà Sữa',
        '0900000005',
        'FEMALE',
        NULL,
        '1992-03-12',
        TRUE,
        NOW()
    );

-- =============================================================================
-- 3. USER_ROLES (Phân quyền)
-- =============================================================================
INSERT INTO
    user_roles (user_id, role_id)
VALUES (
        '00000000-0000-0000-0000-000000000001',
        1
    ), -- Admin
    (
        '00000000-0000-0000-0000-000000000002',
        2
    ), -- User Anh
    (
        '00000000-0000-0000-0000-000000000003',
        2
    ), -- User Binh
    (
        '00000000-0000-0000-0000-000000000004',
        3
    ), -- Merchant 1
    (
        '00000000-0000-0000-0000-000000000005',
        3
    );
-- Merchant 2

-- =============================================================================
-- 4. USER ADDRESSES (Địa chỉ nhận hàng của khách)
-- =============================================================================
INSERT INTO
    addresses (
        id,
        contact_name,
        contact_phone,
        city,
        district,
        ward,
        street,
        note,
        latitude,
        longitude,
        is_default,
        user_id
    )
VALUES (
        1,
        'Nguyễn Văn Anh',
        '0900000002',
        'Hà Nội',
        'Quận Cầu Giấy',
        'Phường Dịch Vọng',
        'Số 12 Ngõ 68 Cầu Giấy',
        'Cổng màu xanh',
        21.0362,
        105.7905,
        TRUE,
        '00000000-0000-0000-0000-000000000002'
    ),
    (
        2,
        'Nguyễn Văn Anh (Cơ quan)',
        '0900000002',
        'Hà Nội',
        'Quận Nam Từ Liêm',
        'Phường Mỹ Đình 1',
        'Tòa nhà Sông Đà, Phạm Hùng',
        'Gọi trước khi giao',
        21.0168,
        105.7788,
        FALSE,
        '00000000-0000-0000-0000-000000000002'
    ),
    (
        3,
        'Trần Thị Bình',
        '0900000003',
        'Hà Nội',
        'Quận Đống Đa',
        'Phường Láng Hạ',
        'Số 45 Láng Hạ',
        'Gửi bảo vệ tòa nhà',
        21.0169,
        105.8148,
        TRUE,
        '00000000-0000-0000-0000-000000000003'
    );

-- =============================================================================
-- 5. MERCHANTS (Cửa hàng)
-- =============================================================================
INSERT INTO
    merchants (
        id,
        user_id,
        merchant_restaurant_name,
        merchant_email,
        merchant_phone,
        merchant_status,
        lock_reason,
        locked_at,
        reject_reason,
        rejected_at,
        is_trusted_partner,
        bank_name,
        bank_account_number
    )
VALUES (
        '10000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000004',
        'Quán Ngon Hà Nội - Phở & Bún Chả',
        'merchant1@gmail.com',
        '0911111111',
        'APPROVED',
        NULL,
        NULL,
        NULL,
        NULL,
        TRUE,
        'Vietcombank',
        '999888777666'
    ),
    (
        '10000000-0000-0000-0000-000000000002',
        '00000000-0000-0000-0000-000000000005',
        'Tiệm Trà & Ăn Vặt Chill',
        'merchant2@gmail.com',
        '0911111112',
        'APPROVED',
        NULL,
        NULL,
        NULL,
        NULL,
        FALSE,
        'Techcombank',
        '19036688990011'
    );

-- =============================================================================
-- 6. MERCHANT ADDRESSES (Địa chỉ quán)
-- =============================================================================
INSERT INTO
    merchant_addresses (
        id,
        merchant_id,
        merchant_address,
        province_code,
        district_code,
        ward_code,
        merchant_open_time,
        merchant_close_time,
        latitude,
        longitude,
        is_default,
        created_at,
        updated_at
    )
VALUES (
        '20000000-0000-0000-0000-000000000001',
        '10000000-0000-0000-0000-000000000001',
        '25 Hàng Bông, Phường Hàng Bông, Quận Hoàn Kiếm, Hà Nội',
        '01',
        '001',
        '00001',
        '06:30:00',
        '22:00:00',
        21.0315,
        105.8475,
        TRUE,
        NOW(),
        NOW()
    ),
    (
        '20000000-0000-0000-0000-000000000002',
        '10000000-0000-0000-0000-000000000002',
        '102 Chùa Láng, Phường Láng Thượng, Quận Đống Đa, Hà Nội',
        '01',
        '006',
        '00184',
        '08:00:00',
        '23:00:00',
        21.0227,
        105.8018,
        TRUE,
        NOW(),
        NOW()
    );

-- =============================================================================
-- 7. FOOD CATEGORIES (Danh mục món)
-- =============================================================================
INSERT INTO
    food_categories (
        id,
        category_name,
        category_description,
        created_at,
        updated_at
    )
VALUES (
        1,
        'Cơm & Món Việt',
        'Các món ăn truyền thống đậm đà vị Việt',
        NOW(),
        NOW()
    ),
    (
        2,
        'Bún / Phở / Mì',
        'Món nước và bún trộn nóng hổi',
        NOW(),
        NOW()
    ),
    (
        3,
        'Đồ Uống & Trà Sữa',
        'Nước giải khát, trà trái cây và trà sữa',
        NOW(),
        NOW()
    ),
    (
        4,
        'Ăn Vặt & Món Phụ',
        'Các món ăn kèm, đồ chiên giòn rụm',
        NOW(),
        NOW()
    );

-- =============================================================================
-- 8. TAGS (Thẻ nhãn)
-- =============================================================================
INSERT INTO
    tags (
        id,
        tag_name,
        created_at,
        updated_at
    )
VALUES (1, 'Bán chạy', NOW(), NOW()),
    (2, 'Món mới', NOW(), NOW()),
    (
        3,
        'Giảm giá cực sốc',
        NOW(),
        NOW()
    ),
    (
        4,
        'Gợi ý hôm nay',
        NOW(),
        NOW()
    ),
    (5, 'Freeship', NOW(), NOW());

-- =============================================================================
-- 9. FOODS (Món ăn)
-- =============================================================================
INSERT INTO
    foods (
        id,
        merchant_id,
        merchant_address_id,
        food_name,
        preparation_time,
        food_note,
        price,
        discount_price,
        service_fee,
        views,
        order_count,
        is_active,
        is_recommended,
        created_at,
        updated_at,
        deleted_at
    )
VALUES (
        1,
        '10000000-0000-0000-0000-000000000001',
        '20000000-0000-0000-0000-000000000001',
        'Phở Bò Tái Nạm',
        15,
        'Nước dùng ninh từ xương ống 12 tiếng, kèm rau sống và quẩy',
        60000.00,
        50000.00,
        5000.00,
        320,
        145,
        TRUE,
        TRUE,
        NOW(),
        NOW(),
        NULL
    ),
    (
        2,
        '10000000-0000-0000-0000-000000000001',
        '20000000-0000-0000-0000-000000000001',
        'Bún Chả Hà Nội Đặc Biệt',
        20,
        'Chả nướng than hoa thơm lừng, đu đủ muối giòn',
        55000.00,
        45000.00,
        5000.00,
        280,
        98,
        TRUE,
        TRUE,
        NOW(),
        NOW(),
        NULL
    ),
    (
        3,
        '10000000-0000-0000-0000-000000000001',
        '20000000-0000-0000-0000-000000000001',
        'Cơm Rang Dưa Bò',
        15,
        'Cơm hạt tơi giòn, dưa chua xào thịt bò mềm',
        50000.00,
        NULL,
        5000.00,
        110,
        42,
        TRUE,
        FALSE,
        NOW(),
        NOW(),
        NULL
    ),
    (
        4,
        '10000000-0000-0000-0000-000000000002',
        '20000000-0000-0000-0000-000000000002',
        'Trà Đào Cam Sả Tươi',
        10,
        'Đồ uống giải nhiệt kèm 3 miếng đào giòn',
        38000.00,
        29000.00,
        3000.00,
        500,
        210,
        TRUE,
        TRUE,
        NOW(),
        NOW(),
        NULL
    ),
    (
        5,
        '10000000-0000-0000-0000-000000000002',
        '20000000-0000-0000-0000-000000000002',
        'Trà Sữa Trân Châu Đường Đen',
        10,
        'Độ ngọt vừa phải, trân châu dẻo mềm',
        42000.00,
        NULL,
        3000.00,
        410,
        185,
        TRUE,
        TRUE,
        NOW(),
        NOW(),
        NULL
    ),
    (
        6,
        '10000000-0000-0000-0000-000000000002',
        '20000000-0000-0000-0000-000000000002',
        'Nem Rán Hà Nội (5 Chiếc)',
        15,
        'Nem nhân thịt mộc nhĩ giòn rụm',
        35000.00,
        NULL,
        3000.00,
        95,
        30,
        TRUE,
        FALSE,
        NOW(),
        NOW(),
        NULL
    );

-- =============================================================================
-- 10. FOOD CATEGORY MAPPING
-- =============================================================================
INSERT INTO
    food_category_mapping (food_id, food_category_id)
VALUES (1, 2), -- Phở Bò -> Bún / Phở / Mì
    (2, 2), -- Bún Chả -> Bún / Phở / Mì
    (3, 1), -- Cơm Rang -> Cơm & Món Việt
    (4, 3), -- Trà Đào -> Đồ Uống & Trà Sữa
    (5, 3), -- Trà Sữa -> Đồ Uống & Trà Sữa
    (6, 4);
-- Nem Rán -> Ăn Vặt & Món Phụ

-- =============================================================================
-- 11. FOOD TAG MAPPING
-- =============================================================================
INSERT INTO
    food_tag_mapping (food_id, tag_id)
VALUES (1, 1),
    (1, 4), -- Phở: Bán chạy, Gợi ý
    (2, 1),
    (2, 3), -- Bún Chả: Bán chạy, Giảm giá
    (4, 1),
    (4, 3),
    (4, 5), -- Trà Đào: Bán chạy, Giảm giá, Freeship
    (5, 1),
    (5, 4), -- Trà Sữa: Bán chạy, Gợi ý
    (6, 2);
-- Nem Rán: Món mới

-- =============================================================================
-- 12. FOOD IMAGES
-- =============================================================================
INSERT INTO
    food_images (
        id,
        food_id,
        image_url,
        is_primary,
        created_at
    )
VALUES (
        1,
        1,
        'https://images.unsplash.com/photo-1582878826629-29b7ad1cdc43',
        TRUE,
        NOW()
    ),
    (
        2,
        2,
        'https://images.unsplash.com/photo-1569058242253-92a9c755a0ec',
        TRUE,
        NOW()
    ),
    (
        3,
        3,
        'https://images.unsplash.com/photo-1603133872878-684f208fb84b',
        TRUE,
        NOW()
    ),
    (
        4,
        4,
        'https://images.unsplash.com/photo-1556679343-c7306c1976bc',
        TRUE,
        NOW()
    ),
    (
        5,
        5,
        'https://images.unsplash.com/photo-1572490122747-3968b75cc699',
        TRUE,
        NOW()
    ),
    (
        6,
        6,
        'https://images.unsplash.com/photo-1541544741938-0af808871cc0',
        TRUE,
        NOW()
    );

-- =============================================================================
-- 13. FOOD LIKES
-- =============================================================================
INSERT INTO
    food_likes (user_id, food_id)
VALUES (
        '00000000-0000-0000-0000-000000000002',
        1
    ),
    (
        '00000000-0000-0000-0000-000000000002',
        4
    ),
    (
        '00000000-0000-0000-0000-000000000003',
        4
    ),
    (
        '00000000-0000-0000-0000-000000000003',
        5
    );

-- =============================================================================
-- 14. MERCHANT LIKES
-- =============================================================================
INSERT INTO
    merchant_likes (user_id, merchant_id)
VALUES (
        '00000000-0000-0000-0000-000000000002',
        '10000000-0000-0000-0000-000000000001'
    ),
    (
        '00000000-0000-0000-0000-000000000003',
        '10000000-0000-0000-0000-000000000002'
    );

-- =============================================================================
-- 15. VOUCHERS (Mã giảm giá)
-- =============================================================================
INSERT INTO
    vouchers (
        id,
        merchant_id,
        voucher_code,
        discount_type,
        discount_value,
        start_at,
        end_at,
        usage_limit,
        used_count,
        is_active,
        created_at,
        updated_at
    )
VALUES (
        1,
        '10000000-0000-0000-0000-000000000001',
        'GIAM10K',
        'FIXED',
        10000.00,
        NOW(),
        DATE_ADD(NOW(), INTERVAL 30 DAY),
        100,
        5,
        TRUE,
        NOW(),
        NOW()
    ),
    (
        2,
        '10000000-0000-0000-0000-000000000001',
        'GIAM20%',
        'PERCENT',
        20.00,
        NOW(),
        DATE_ADD(NOW(), INTERVAL 30 DAY),
        50,
        2,
        TRUE,
        NOW(),
        NOW()
    ),
    (
        3,
        '10000000-0000-0000-0000-000000000002',
        'GIAM50PT',
        'PERCENT',
        50.00,
        NOW(),
        DATE_ADD(NOW(), INTERVAL 30 DAY),
        30,
        0,
        TRUE,
        NOW(),
        NOW()
    );

-- =============================================================================
-- 15.1 FOOD_VOUCHERS (Món ăn áp dụng mã giảm giá)
-- =============================================================================
INSERT INTO food_vouchers (food_id, voucher_id) VALUES
    (1, 1),
    (3, 1),
    (2, 2),
    (4, 3),
    (5, 3);

-- =============================================================================
-- 16. DELIVERY_PARTNERS (Đối tác vận chuyển mẫu)
-- =============================================================================
INSERT INTO
    delivery_partners (
        id,
        partner_code,
        partner_name,
        email,
        phone,
        address,
        logo_url,
        base_fee,
        base_distance_km,
        fee_per_km,
        peak_multiplier,
        status,
        created_at,
        updated_at
    )
VALUES (
        '20000000-0000-0000-0000-000000000001',
        'GRAB_EXPRESS',
        'GrabExpress Siêu Tốc',
        'contact@grab.vn',
        '19001122',
        'Tòa nhà MapleTree, Quận 7, TP.HCM',
        'https://upload.wikimedia.org/wikipedia/commons/thumb/b/b2/Grab_Logo.svg/512px-Grab_Logo.svg.png',
        16000.00,
        3.0,
        5000.00,
        1.20,
        'ACTIVE',
        NOW(),
        NOW()
    ),
    (
        '20000000-0000-0000-0000-000000000002',
        'SHOPEE_FOOD',
        'ShopeeXpress Instant',
        'spx@shopee.vn',
        '19001221',
        'Saigon Centre, Quận 1, TP.HCM',
        'https://brandeps.com/logo-download/S/Shopee-logo-vector-01.svg',
        14000.00,
        3.0,
        4500.00,
        1.15,
        'ACTIVE',
        NOW(),
        NOW()
    ),
    (
        '20000000-0000-0000-0000-000000000003',
        'AHAMOVE',
        'Ahamove Tiết Kiệm',
        'support@ahamove.com',
        '19005454',
        'Tòa nhà Rivera Park, Đống Đa, Hà Nội',
        'https://upload.wikimedia.org/wikipedia/commons/thumb/c/ca/Ahamove_logo.png/512px-Ahamove_logo.png',
        15000.00,
        3.5,
        4800.00,
        1.20,
        'ACTIVE',
        NOW(),
        NOW()
    ),
    (
        '20000000-0000-0000-0000-000000000004',
        'BE_DELIVERY',
        'BeDelivery Hỏa Tốc',
        'hotro@be.com.vn',
        '19002323',
        'Tòa nhà Charmvit, Cầu Giấy, Hà Nội',
        'https://be.com.vn/wp-content/uploads/2021/04/logo-be-yellow.png',
        17000.00,
        3.0,
        5200.00,
        1.10,
        'ACTIVE',
        NOW(),
        NOW()
    );

-- =============================================================================
-- 17. ORDERS (Đơn hàng mẫu)
-- =============================================================================
INSERT INTO
    orders (
        id,
        order_code,
        user_id,
        merchant_id,
        delivery_partner_id,
        contact_name,
        contact_phone,
        delivery_address,
        note,
        status,
        payment_method,
        subtotal_price,
        shipping_fee,
        service_fee,
        discount_amount,
        total_amount,
        cancel_reason,
        estimated_delivery_time,
        created_at,
        updated_at
    )
VALUES (
        1,
        'MC-20260821-001',
        '00000000-0000-0000-0000-000000000002',
        '10000000-0000-0000-0000-000000000001',
        '20000000-0000-0000-0000-000000000001',
        'Nguyễn Văn Anh',
        '0900000002',
        'Số 12 Ngõ 68 Cầu Giấy, Phường Dịch Vọng, Quận Cầu Giấy, Hà Nội',
        'Cho nhiều tương ớt và quẩy giòn',
        'PENDING',
        'COD',
        95000.00,
        16000.00,
        5000.00,
        0.00,
        116000.00,
        NULL,
        DATE_ADD(NOW(), INTERVAL 30 MINUTE),
        NOW(),
        NOW()
    ),
    (
        2,
        'MC-20260821-002',
        '00000000-0000-0000-0000-000000000003',
        '10000000-0000-0000-0000-000000000001',
        '20000000-0000-0000-0000-000000000002',
        'Trần Thị Bình',
        '0900000003',
        'Số 45 Láng Hạ, Phường Láng Hạ, Quận Đống Đa, Hà Nội',
        'Giao giờ hành chính',
        'PREPARING',
        'CARD',
        100000.00,
        14000.00,
        5000.00,
        0.00,
        119000.00,
        NULL,
        DATE_ADD(NOW(), INTERVAL 20 MINUTE),
        DATE_SUB(NOW(), INTERVAL 15 MINUTE),
        NOW()
    ),
    (
        3,
        'MC-20260820-003',
        '00000000-0000-0000-0000-000000000002',
        '10000000-0000-0000-0000-000000000001',
        '20000000-0000-0000-0000-000000000001',
        'Nguyễn Văn Anh',
        '0900000002',
        'Số 12 Ngõ 68 Cầu Giấy, Phường Dịch Vọng, Quận Cầu Giấy, Hà Nội',
        '',
        'COMPLETED',
        'COD',
        50000.00,
        16000.00,
        5000.00,
        0.00,
        71000.00,
        NULL,
        DATE_SUB(NOW(), INTERVAL 2 HOUR),
        DATE_SUB(NOW(), INTERVAL 3 HOUR),
        DATE_SUB(NOW(), INTERVAL 2 HOUR)
    ),
    (
        4,
        'MC-20260821-004',
        '00000000-0000-0000-0000-000000000002',
        '10000000-0000-0000-0000-000000000002',
        '20000000-0000-0000-0000-000000000003',
        'Nguyễn Văn Anh',
        '0900000002',
        'Tòa nhà Sông Đà, Phạm Hùng, Phường Mỹ Đình 1, Quận Nam Từ Liêm, Hà Nội',
        'Trà ít đường, nhiều đá',
        'PENDING',
        'COD',
        93000.00,
        15000.00,
        3000.00,
        0.00,
        111000.00,
        NULL,
        DATE_ADD(NOW(), INTERVAL 25 MINUTE),
        NOW(),
        NOW()
    );

-- =============================================================================
-- 18. ORDER_ITEMS (Chi tiết món ăn trong đơn hàng)
-- =============================================================================
INSERT INTO
    order_items (
        id,
        order_id,
        food_id,
        food_name,
        food_image,
        price,
        quantity,
        subtotal,
        note
    )
VALUES (
        1,
        1,
        1,
        'Phở Bò Tái Nạm',
        'https://images.unsplash.com/photo-1582878826629-29b7ad1cdc43',
        50000.00,
        1,
        50000.00,
        'Không hành'
    ),
    (
        2,
        1,
        2,
        'Bún Chả Hà Nội Đặc Biệt',
        'https://images.unsplash.com/photo-1569058242253-92a9c755a0ec',
        45000.00,
        1,
        45000.00,
        'Thêm bún'
    ),
    (
        3,
        2,
        3,
        'Cơm Rang Dưa Bò',
        'https://images.unsplash.com/photo-1603133872878-684f208fb84b',
        50000.00,
        2,
        100000.00,
        'Nhiều dưa chua'
    ),
    (
        4,
        3,
        1,
        'Phở Bò Tái Nạm',
        'https://images.unsplash.com/photo-1582878826629-29b7ad1cdc43',
        50000.00,
        1,
        50000.00,
        NULL
    ),
    (
        5,
        4,
        4,
        'Trà Đào Cam Sả Tươi',
        'https://images.unsplash.com/photo-1556679343-c7306c1976bc',
        29000.00,
        2,
        58000.00,
        '30% đường'
    ),
    (
        6,
        4,
        6,
        'Nem Rán Hà Nội (5 Chiếc)',
        'https://images.unsplash.com/photo-1541544741938-0af808871cc0',
        35000.00,
        1,
        35000.00,
        NULL
    );

-- =============================================================================
-- 19. SAMPLE STATS ORDERS & ORDER ITEMS FOR MERCHANT 1
-- =============================================================================
INSERT INTO orders (id, order_code, user_id, merchant_id, contact_name, contact_phone, delivery_address, status, payment_method, subtotal_price, shipping_fee, service_fee, discount_amount, total_amount, completed_at, created_at) VALUES
(101, 'MC-20260115-01', '00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'Nguyễn Văn Anh', '0900000002', 'Số 12 Ngõ 68 Cầu Giấy, Phường Dịch Vọng, Quận Cầu Giấy, Hà Nội', 'COMPLETED', 'COD', 170000, 15000, 5000, 0, 190000, '2026-01-15 10:30:00', '2026-01-15 10:00:00'),
(102, 'MC-20260218-02', '00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', 'Trần Thị Bình', '0900000003', 'Số 45 Láng Hạ, Phường Láng Hạ, Quận Đống Đa, Hà Nội', 'COMPLETED', 'CARD', 220000, 15000, 5000, 0, 240000, '2026-02-18 12:00:00', '2026-02-18 11:30:00'),
(103, 'MC-20260310-03', '00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'Nguyễn Văn Anh', '0900000002', 'Số 12 Ngõ 68 Cầu Giấy, Phường Dịch Vọng, Quận Cầu Giấy, Hà Nội', 'COMPLETED', 'COD', 175000, 15000, 5000, 0, 195000, '2026-03-10 12:45:00', '2026-03-10 12:15:00'),
(104, 'MC-20260405-04', '00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'Nguyễn Văn Anh', '0900000002', 'Số 12 Ngõ 68 Cầu Giấy, Phường Dịch Vọng, Quận Cầu Giấy, Hà Nội', 'COMPLETED', 'COD', 290000, 15000, 5000, 0, 310000, '2026-04-05 18:30:00', '2026-04-05 18:00:00'),
(105, 'MC-20260520-05', '00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', 'Trần Thị Bình', '0900000003', 'Số 45 Láng Hạ, Phường Láng Hạ, Quận Đống Đa, Hà Nội', 'COMPLETED', 'CARD', 310000, 15000, 5000, 0, 330000, '2026-05-20 20:00:00', '2026-05-20 19:30:00'),
(106, 'MC-20260612-06', '00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'Nguyễn Văn Anh', '0900000002', 'Số 12 Ngõ 68 Cầu Giấy, Phường Dịch Vọng, Quận Cầu Giấy, Hà Nội', 'COMPLETED', 'COD', 270000, 15000, 5000, 0, 290000, '2026-06-12 12:30:00', '2026-06-12 12:00:00'),
(107, 'MC-20260725-07', '00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'Nguyễn Văn Anh', '0900000002', 'Số 12 Ngõ 68 Cầu Giấy, Phường Dịch Vọng, Quận Cầu Giấy, Hà Nội', 'COMPLETED', 'COD', 350000, 15000, 5000, 0, 370000, '2026-07-25 19:15:00', '2026-07-25 18:45:00'),
(108, 'MC-20260820-08', '00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'Nguyễn Văn Anh', '0900000002', 'Số 12 Ngõ 68 Cầu Giấy, Phường Dịch Vọng, Quận Cầu Giấy, Hà Nội', 'COMPLETED', 'COD', 340000, 15000, 5000, 0, 360000, '2026-08-20 19:45:00', '2026-08-20 19:15:00');

INSERT INTO order_items (id, order_id, food_id, food_name, price, quantity, subtotal) VALUES
(101, 101, 1, 'Phở Bò Tái Nạm', 60000, 2, 120000),
(102, 101, 2, 'Bún Chả Hà Nội Đặc Biệt', 50000, 1, 50000),
(103, 102, 1, 'Phở Bò Tái Nạm', 60000, 2, 120000),
(104, 102, 4, 'Trà Đào Cam Sả Tươi', 25000, 4, 100000),
(105, 103, 2, 'Bún Chả Hà Nội Đặc Biệt', 50000, 3, 150000),
(106, 103, 4, 'Trà Đào Cam Sả Tươi', 25000, 1, 25000),
(107, 104, 1, 'Phở Bò Tái Nạm', 60000, 4, 240000),
(108, 104, 2, 'Bún Chả Hà Nội Đặc Biệt', 50000, 1, 50000),
(109, 105, 1, 'Phở Bò Tái Nạm', 60000, 3, 180000),
(110, 105, 2, 'Bún Chả Hà Nội Đặc Biệt', 50000, 2, 100000),
(111, 106, 2, 'Bún Chả Hà Nội Đặc Biệt', 50000, 3, 150000),
(112, 106, 1, 'Phở Bò Tái Nạm', 60000, 2, 120000),
(113, 107, 1, 'Phở Bò Tái Nạm', 60000, 5, 300000),
(114, 107, 4, 'Trà Đào Cam Sả Tươi', 25000, 2, 50000),
(115, 108, 1, 'Phở Bò Tái Nạm', 60000, 4, 240000),
(116, 108, 4, 'Trà Đào Cam Sả Tươi', 25000, 4, 100000);

-- =============================================================================
-- 20. SAMPLE ORDERS FOR WEEK 35 & 36 (TUẦN & THÁNG 8, 9/2026)
-- =============================================================================
-- Tuần 35 (24/08 - 30/08/2026)
INSERT INTO orders (
    id, order_code, user_id, merchant_id, delivery_partner_id, contact_name, contact_phone, 
    delivery_address, status, payment_method, subtotal_price, shipping_fee, service_fee, 
    discount_amount, total_amount, cancel_reason, completed_at, created_at, updated_at
) VALUES
(201, 'MC-20260826-01', '00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'Nguyễn Văn Anh', '0900000002', 'Số 12 Ngõ 68 Cầu Giấy, Hà Nội', 'COMPLETED', 'COD', 120000.00, 16000.00, 5000.00, 0.00, 141000.00, NULL, '2026-08-26 12:30:00', '2026-08-26 11:45:00', '2026-08-26 12:30:00'),
(202, 'MC-20260828-02', '00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', 'Trần Thị Bình', '0900000003', 'Số 45 Láng Hạ, Hà Nội', 'COMPLETED', 'CARD', 180000.00, 15000.00, 5000.00, 20000.00, 180000.00, NULL, '2026-08-28 19:15:00', '2026-08-28 18:30:00', '2026-08-28 19:15:00'),
(203, 'MC-20260830-03', '00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'Nguyễn Văn Anh', '0900000002', 'Số 12 Ngõ 68 Cầu Giấy, Hà Nội', 'COMPLETED', 'COD', 250000.00, 18000.00, 5000.00, 0.00, 273000.00, NULL, '2026-08-30 14:00:00', '2026-08-30 13:10:00', '2026-08-30 14:00:00');

-- Tuần 36 (31/08 - 06/09/2026 - Đang diễn ra)
INSERT INTO orders (
    id, order_code, user_id, merchant_id, delivery_partner_id, contact_name, contact_phone, 
    delivery_address, status, payment_method, subtotal_price, shipping_fee, service_fee, 
    discount_amount, total_amount, cancel_reason, completed_at, created_at, updated_at
) VALUES
(204, 'MC-20260831-04', '00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', 'Trần Thị Bình', '0900000003', 'Số 45 Láng Hạ, Hà Nội', 'COMPLETED', 'COD', 95000.00, 14000.00, 5000.00, 0.00, 114000.00, NULL, '2026-08-31 12:40:00', '2026-08-31 12:00:00', '2026-08-31 12:40:00'),
(205, 'MC-20260901-05', '00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'Nguyễn Văn Anh', '0900000002', 'Số 12 Ngõ 68 Cầu Giấy, Hà Nội', 'COMPLETED', 'CARD', 150000.00, 16000.00, 5000.00, 0.00, 171000.00, NULL, '2026-09-01 19:20:00', '2026-09-01 18:40:00', '2026-09-01 19:20:00'),
(206, 'MC-20260902-06', '00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'Nguyễn Văn Anh', '0900000002', 'Số 12 Ngõ 68 Cầu Giấy, Hà Nội', 'PREPARING', 'COD', 85000.00, 15000.00, 5000.00, 0.00, 105000.00, NULL, NULL, NOW(), NOW()),
(207, 'MC-20260902-07', '00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', 'Trần Thị Bình', '0900000003', 'Số 45 Láng Hạ, Hà Nội', 'DELIVERING', 'CARD', 110000.00, 14000.00, 5000.00, 0.00, 129000.00, NULL, NULL, NOW(), NOW()),
(208, 'MC-20260901-08', '00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'Nguyễn Văn Anh', '0900000002', 'Số 12 Ngõ 68 Cầu Giấy, Hà Nội', 'CANCELLED', 'COD', 70000.00, 15000.00, 5000.00, 0.00, 90000.00, 'Khách hàng đổi ý muốn đổi món khác', NULL, '2026-09-01 10:15:00', '2026-09-01 10:20:00');

-- Đơn cho Merchant 2 (Tiệm Trà & Ăn Vặt)
INSERT INTO orders (
    id, order_code, user_id, merchant_id, delivery_partner_id, contact_name, contact_phone, 
    delivery_address, status, payment_method, subtotal_price, shipping_fee, service_fee, 
    discount_amount, total_amount, cancel_reason, completed_at, created_at, updated_at
) VALUES
(209, 'MC-20260827-09', '00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000003', 'Nguyễn Văn Anh', '0900000002', 'Tòa Sông Đà, Mỹ Đình, Hà Nội', 'COMPLETED', 'COD', 90000.00, 15000.00, 3000.00, 0.00, 108000.00, NULL, '2026-08-27 15:45:00', '2026-08-27 15:00:00', '2026-08-27 15:45:00'),
(210, 'MC-20260901-10', '00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000003', 'Trần Thị Bình', '0900000003', 'Tòa Sông Đà, Mỹ Đình, Hà Nội', 'COMPLETED', 'CARD', 115000.00, 15000.00, 3000.00, 10000.00, 123000.00, NULL, '2026-09-01 16:30:00', '2026-09-01 15:50:00', '2026-09-01 16:30:00');

-- =============================================================================
-- 21. CHI TIẾT MÓN ĂN CHO ĐƠN HÀNG MỚI
-- =============================================================================
INSERT INTO order_items (id, order_id, food_id, food_name, price, quantity, subtotal, note) VALUES
(201, 201, 1, 'Phở Bò Tái Nạm', 60000.00, 2, 120000.00, 'Nhiều tương ớt'),
(202, 202, 2, 'Bún Chả Hà Nội Đặc Biệt', 50000.00, 2, 100000.00, 'Thêm bún'),
(203, 202, 4, 'Trà Đào Cam Sả Tươi', 25000.00, 2, 50000.00, 'Ít đá'),
(204, 202, 6, 'Nem Rán Hà Nội (5 Chiếc)', 30000.00, 1, 30000.00, NULL),
(205, 203, 1, 'Phở Bò Tái Nạm', 60000.00, 3, 180000.00, NULL),
(206, 203, 6, 'Nem Rán Hà Nội (5 Chiếc)', 35000.00, 2, 70000.00, 'Rán giòn'),
(207, 204, 2, 'Bún Chả Hà Nội Đặc Biệt', 50000.00, 1, 50000.00, NULL),
(208, 204, 6, 'Nem Rán Hà Nội (5 Chiếc)', 45000.00, 1, 45000.00, NULL),
(209, 205, 1, 'Phở Bò Tái Nạm', 60000.00, 2, 120000.00, 'Không hành'),
(210, 205, 4, 'Trà Đào Cam Sả Tươi', 30000.00, 1, 30000.00, NULL),
(211, 206, 1, 'Phở Bò Tái Nạm', 60000.00, 1, 60000.00, NULL),
(212, 206, 4, 'Trà Đào Cam Sả Tươi', 25000.00, 1, 25000.00, NULL),
(213, 207, 2, 'Bún Chả Hà Nội Đặc Biệt', 55000.00, 2, 110000.00, NULL),
(214, 208, 1, 'Phở Bò Tái Nạm', 70000.00, 1, 70000.00, NULL),
(215, 209, 4, 'Trà Đào Cam Sả Tươi', 30000.00, 3, 90000.00, '30% đường'),
(216, 210, 4, 'Trà Đào Cam Sả Tươi', 30000.00, 3, 90000.00, '50% đường'),
(217, 210, 6, 'Nem Rán Hà Nội (5 Chiếc)', 25000.00, 1, 25000.00, NULL);

-- =============================================================================
-- 22. KỲ ĐỐI SOÁT MẪU (MERCHANT_SETTLEMENTS)
-- =============================================================================
INSERT INTO merchant_settlements (
    id, merchant_id, period_key, period_type, start_date, end_date, 
    total_gross_revenue, total_discount, commission_rate, total_commission_fee, 
    net_revenue, total_orders, adjustment_amount, status, confirmed_at, created_at, updated_at
) VALUES
(10, '10000000-0000-0000-0000-000000000001', '2026-07', 'MONTH', '2026-07-01 00:00:00', '2026-08-01 00:00:00', 350000.00, 0.00, 0.000010, 3.50, 349996.50, 1, 0.00, 'CONFIRMED', '2026-08-02 10:15:00', '2026-08-01 00:05:00', '2026-08-02 10:15:00'),
(11, '10000000-0000-0000-0000-000000000001', '2026-08', 'MONTH', '2026-08-01 00:00:00', '2026-09-01 00:00:00', 890000.00, 20000.00, 0.000010, 8.70, 869991.30, 4, 0.00, 'PENDING_CONFIRMATION', NULL, '2026-09-01 00:05:00', '2026-09-01 00:05:00'),
(12, '10000000-0000-0000-0000-000000000001', '2026-W34', 'WEEK', '2026-08-17 00:00:00', '2026-08-24 00:00:00', 340000.00, 0.00, 0.000010, 3.40, 339996.60, 1, 0.00, 'CONFIRMED', '2026-08-25 09:00:00', '2026-08-24 00:05:00', '2026-08-25 09:00:00'),
(13, '10000000-0000-0000-0000-000000000001', '2026-W35', 'WEEK', '2026-08-24 00:00:00', '2026-08-31 00:00:00', 550000.00, 20000.00, 0.000010, 5.30, 529994.70, 3, 0.00, 'DISPUTED', NULL, '2026-08-31 00:05:00', '2026-08-31 10:20:00');

-- =============================================================================
-- 23. KHIẾU NẠI ĐỐI SOÁT (SETTLEMENT_CLAIMS)
-- =============================================================================
INSERT INTO settlement_claims (
    id, settlement_id, merchant_id, reason, description, evidence_image_url, 
    adjustment_amount, status, admin_note, created_at, updated_at
) VALUES
(1, 13, '10000000-0000-0000-0000-000000000001', 'COMMISSION_FEE_MISMATCH', 'Phí chiết khấu kỳ tuần 35 chưa áp dụng đúng chính sách ưu đãi hợp đồng mới cho nhóm món phở đặc biệt. Nhờ Admin kiểm tra và hoàn lại 50.000 đ.', 'https://images.unsplash.com/photo-1554224155-8d04cb21cd6c?w=800', 0.00, 'PENDING', NULL, '2026-08-31 10:20:00', '2026-08-31 10:20:00'),
(2, 10, '10000000-0000-0000-0000-000000000001', 'OTHER', 'Đề nghị hỗ trợ chi phí mã giảm giá đồng tài trợ trong chiến dịch ngày hội ẩm thực.', 'https://images.unsplash.com/photo-1559526324-4b87b5e36e44?w=800', 30000.00, 'RESOLVED', 'Admin đã kiểm tra chương trình đồng tài trợ và chấp thuận bồi hoàn thêm 30.000 VNĐ.', '2026-08-01 14:00:00', '2026-08-02 09:30:00');

-- =============================================================================
-- 24. YÊU CẦU RÚT TIỀN (MERCHANT_PAYOUT_REQUESTS)
-- =============================================================================
INSERT INTO merchant_payout_requests (
    id, merchant_id, type, amount, bank_name, bank_account_number, 
    status, admin_note, transfer_proof_url, created_at, completed_at, rejected_at
) VALUES
('30000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'WITHDRAWAL', 300000, 'Vietcombank', '987654321098', 'COMPLETED', 'Đã thực hiện lệnh chuyển khoản qua ngân hàng Vietcombank cho quán. Mã giao dịch VCB-883921.', 'https://images.unsplash.com/photo-1559526324-4b87b5e36e44?w=800', '2026-08-15 14:00:00', '2026-08-15 16:30:00', NULL),
('30000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'WITHDRAWAL', 250000, 'Vietcombank', '987654321098', 'PENDING', NULL, NULL, NOW(), NULL, NULL),
('30000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', 'WITHDRAWAL', 150000, 'Vietcombank', '987654321098', 'REJECTED', 'Số tài khoản ngân hàng không trùng khớp với tên pháp nhân đăng ký ban đầu. Quán vui lòng vào Cài đặt tài khoản cập nhật lại.', NULL, '2026-08-10 09:00:00', NULL, '2026-08-10 11:30:00');

-- =============================================================================
-- 25. YÊU CẦU ĐỐI TÁC THÂN THIẾT (TRUSTED_PARTNER_REQUESTS)
-- =============================================================================
INSERT INTO trusted_partner_requests (
    id, merchant_id, status, revenue, reject_reason, created_at, reviewed_at
) VALUES
(1, '10000000-0000-0000-0000-000000000001', 'PENDING', 105000000.00, NULL, '2026-09-01 09:30:00', NULL),
(2, '10000000-0000-0000-0000-000000000002', 'REJECTED', 85000000.00, 'Doanh thu tháng chưa đạt ngưỡng tối thiểu 100.000.000 VNĐ theo tiêu chuẩn quy định.', '2026-08-15 11:00:00', '2026-08-16 14:00:00');
