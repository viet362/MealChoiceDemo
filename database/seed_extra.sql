-- =============================================================================
-- MEALCHOICE EXTRA SEED DATA (DỮ LIỆU BỔ SUNG CHO TÍNH NĂNG ĐỐI SOÁT)
-- =============================================================================

-- 1. Cập nhật completed_at cho các đơn COMPLETED có sẵn
UPDATE orders
SET
    completed_at = created_at,
    updated_at = created_at
WHERE
    status = 'COMPLETED'
    AND completed_at IS NULL;

-- 2. Thêm đơn hàng mới cho tuần 35 và tuần 36 (tháng 8 và 9/2026)
-- Tuần 35 (24/08 - 30/08/2026)
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
        status,
        payment_method,
        subtotal_price,
        shipping_fee,
        service_fee,
        discount_amount,
        total_amount,
        cancel_reason,
        completed_at,
        created_at,
        updated_at
    )
VALUES
    (201, 'MC-20260826-01', '00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'Nguyễn Văn Anh', '0900000002', 'Số 12 Ngõ 68 Cầu Giấy, Hà Nội', 'COMPLETED', 'COD', 120000.00, 16000.00, 5000.00, 0.00, 141000.00, NULL, '2026-08-26 12:30:00', '2026-08-26 11:45:00', '2026-08-26 12:30:00'),
    (202, 'MC-20260828-02', '00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', 'Trần Thị Bình', '0900000003', 'Số 45 Láng Hạ, Hà Nội', 'COMPLETED', 'CARD', 180000.00, 15000.00, 5000.00, 20000.00, 180000.00, NULL, '2026-08-28 19:15:00', '2026-08-28 18:30:00', '2026-08-28 19:15:00'),
    (203, 'MC-20260830-03', '00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'Nguyễn Văn Anh', '0900000002', 'Số 12 Ngõ 68 Cầu Giấy, Hà Nội', 'COMPLETED', 'COD', 250000.00, 18000.00, 5000.00, 0.00, 273000.00, NULL, '2026-08-30 14:00:00', '2026-08-30 13:10:00', '2026-08-30 14:00:00')
ON DUPLICATE KEY UPDATE
    updated_at = VALUES(updated_at);

-- Tuần 36 (31/08 - 06/09/2026 - Đang diễn ra)
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
        status,
        payment_method,
        subtotal_price,
        shipping_fee,
        service_fee,
        discount_amount,
        total_amount,
        cancel_reason,
        completed_at,
        created_at,
        updated_at
    )
VALUES
    (204, 'MC-20260831-04', '00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', 'Trần Thị Bình', '0900000003', 'Số 45 Láng Hạ, Hà Nội', 'COMPLETED', 'COD', 95000.00, 14000.00, 5000.00, 0.00, 114000.00, NULL, '2026-08-31 12:40:00', '2026-08-31 12:00:00', '2026-08-31 12:40:00'),
    (205, 'MC-20260901-05', '00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'Nguyễn Văn Anh', '0900000002', 'Số 12 Ngõ 68 Cầu Giấy, Hà Nội', 'COMPLETED', 'CARD', 150000.00, 16000.00, 5000.00, 0.00, 171000.00, NULL, '2026-09-01 19:20:00', '2026-09-01 18:40:00', '2026-09-01 19:20:00'),
    (206, 'MC-20260902-06', '00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'Nguyễn Văn Anh', '0900000002', 'Số 12 Ngõ 68 Cầu Giấy, Hà Nội', 'PREPARING', 'COD', 85000.00, 15000.00, 5000.00, 0.00, 105000.00, NULL, NULL, NOW(), NOW()),
    (207, 'MC-20260902-07', '00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', 'Trần Thị Bình', '0900000003', 'Số 45 Láng Hạ, Hà Nội', 'DELIVERING', 'CARD', 110000.00, 14000.00, 5000.00, 0.00, 129000.00, NULL, NULL, NOW(), NOW()),
    (208, 'MC-20260901-08', '00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'Nguyễn Văn Anh', '0900000002', 'Số 12 Ngõ 68 Cầu Giấy, Hà Nội', 'CANCELLED', 'COD', 70000.00, 15000.00, 5000.00, 0.00, 90000.00, 'Khách hàng đổi ý muốn đổi món khác', NULL, '2026-09-01 10:15:00', '2026-09-01 10:20:00')
ON DUPLICATE KEY UPDATE
    updated_at = VALUES(updated_at);

-- Đơn cho Merchant 2 (Tiệm Trà & Ăn Vặt)
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
        status,
        payment_method,
        subtotal_price,
        shipping_fee,
        service_fee,
        discount_amount,
        total_amount,
        cancel_reason,
        completed_at,
        created_at,
        updated_at
    )
VALUES
    (209, 'MC-20260827-09', '00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000003', 'Nguyễn Văn Anh', '0900000002', 'Tòa Sông Đà, Mỹ Đình, Hà Nội', 'COMPLETED', 'COD', 90000.00, 15000.00, 3000.00, 0.00, 108000.00, NULL, '2026-08-27 15:45:00', '2026-08-27 15:00:00', '2026-08-27 15:45:00'),
    (210, 'MC-20260901-10', '00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000003', 'Trần Thị Bình', '0900000003', 'Tòa Sông Đà, Mỹ Đình, Hà Nội', 'COMPLETED', 'CARD', 115000.00, 15000.00, 3000.00, 10000.00, 123000.00, NULL, '2026-09-01 16:30:00', '2026-09-01 15:50:00', '2026-09-01 16:30:00')
ON DUPLICATE KEY UPDATE
    updated_at = VALUES(updated_at);

-- 3. Thêm chi tiết món ăn (Order Items)
INSERT INTO
    order_items (
        id,
        order_id,
        food_id,
        food_name,
        price,
        quantity,
        subtotal,
        note
    )
VALUES (
        201,
        201,
        1,
        'Phở Bò Tái Nạm',
        60000.00,
        2,
        120000.00,
        'Nhiều tương ớt'
    ),
    (
        202,
        202,
        2,
        'Bún Chả Hà Nội Đặc Biệt',
        50000.00,
        2,
        100000.00,
        'Thêm bún'
    ),
    (
        203,
        202,
        4,
        'Trà Đào Cam Sả Tươi',
        25000.00,
        2,
        50000.00,
        'Ít đá'
    ),
    (
        204,
        202,
        6,
        'Nem Rán Hà Nội (5 Chiếc)',
        30000.00,
        1,
        30000.00,
        NULL
    ),
    (
        205,
        203,
        1,
        'Phở Bò Tái Nạm',
        60000.00,
        3,
        180000.00,
        NULL
    ),
    (
        206,
        203,
        6,
        'Nem Rán Hà Nội (5 Chiếc)',
        35000.00,
        2,
        70000.00,
        'Rán giòn'
    ),
    (
        207,
        204,
        2,
        'Bún Chả Hà Nội Đặc Biệt',
        50000.00,
        1,
        50000.00,
        NULL
    ),
    (
        208,
        204,
        6,
        'Nem Rán Hà Nội (5 Chiếc)',
        45000.00,
        1,
        45000.00,
        NULL
    ),
    (
        209,
        205,
        1,
        'Phở Bò Tái Nạm',
        60000.00,
        2,
        120000.00,
        'Không hành'
    ),
    (
        210,
        205,
        4,
        'Trà Đào Cam Sả Tươi',
        30000.00,
        1,
        30000.00,
        NULL
    ),
    (
        211,
        206,
        1,
        'Phở Bò Tái Nạm',
        60000.00,
        1,
        60000.00,
        NULL
    ),
    (
        212,
        206,
        4,
        'Trà Đào Cam Sả Tươi',
        25000.00,
        1,
        25000.00,
        NULL
    ),
    (
        213,
        207,
        2,
        'Bún Chả Hà Nội Đặc Biệt',
        55000.00,
        2,
        110000.00,
        NULL
    ),
    (
        214,
        208,
        1,
        'Phở Bò Tái Nạm',
        70000.00,
        1,
        70000.00,
        NULL
    ),
    (
        215,
        209,
        4,
        'Trà Đào Cam Sả Tươi',
        30000.00,
        3,
        90000.00,
        '30% đường'
    ),
    (
        216,
        210,
        4,
        'Trà Đào Cam Sả Tươi',
        30000.00,
        3,
        90000.00,
        '50% đường'
    ),
    (
        217,
        210,
        6,
        'Nem Rán Hà Nội (5 Chiếc)',
        25000.00,
        1,
        25000.00,
        NULL
    )
ON DUPLICATE KEY UPDATE
    subtotal = VALUES(subtotal);

-- 4. Thêm các Kỳ Đối Soát Mẫu (Merchant Settlements)
-- Xóa tạm các kỳ mẫu cũ nếu có trùng key để nạp dữ liệu chuẩn
DELETE FROM merchant_settlements
WHERE
    merchant_id = '10000000-0000-0000-0000-000000000001'
    AND period_key IN (
        '2026-07',
        '2026-08',
        '2026-W34',
        '2026-W35'
    );

INSERT INTO
    merchant_settlements (
        id,
        merchant_id,
        period_key,
        period_type,
        start_date,
        end_date,
        total_gross_revenue,
        total_discount,
        commission_rate,
        total_commission_fee,
        net_revenue,
        total_orders,
        adjustment_amount,
        status,
        confirmed_at,
        created_at,
        updated_at
    )
VALUES
    (10, '10000000-0000-0000-0000-000000000001', '2026-07', 'MONTH', '2026-07-01 00:00:00', '2026-08-01 00:00:00', 350000.00, 0.00, 0.000010, 3.50, 349996.50, 1, 0.00, 'CONFIRMED', '2026-08-02 10:15:00', '2026-08-01 00:05:00', '2026-08-02 10:15:00'),
    (11, '10000000-0000-0000-0000-000000000001', '2026-08', 'MONTH', '2026-08-01 00:00:00', '2026-09-01 00:00:00', 890000.00, 20000.00, 0.000010, 8.70, 869991.30, 4, 0.00, 'PENDING_CONFIRMATION', NULL, '2026-09-01 00:05:00', '2026-09-01 00:05:00'),
    (12, '10000000-0000-0000-0000-000000000001', '2026-W34', 'WEEK', '2026-08-17 00:00:00', '2026-08-24 00:00:00', 340000.00, 0.00, 0.000010, 3.40, 339996.60, 1, 0.00, 'CONFIRMED', '2026-08-25 09:00:00', '2026-08-24 00:05:00', '2026-08-25 09:00:00'),
    (13, '10000000-0000-0000-0000-000000000001', '2026-W35', 'WEEK', '2026-08-24 00:00:00', '2026-08-31 00:00:00', 550000.00, 20000.00, 0.000010, 5.30, 529994.70, 3, 0.00, 'DISPUTED', NULL, '2026-08-31 00:05:00', '2026-08-31 10:20:00')
ON DUPLICATE KEY UPDATE
    net_revenue = VALUES(net_revenue),
    status = VALUES(status);

-- 5. Thêm Khiếu Nại Đối Soát (Settlement Claims)
DELETE FROM settlement_claims
WHERE
    merchant_id = '10000000-0000-0000-0000-000000000001'
    AND id IN (1, 2);

INSERT INTO
    settlement_claims (
        id,
        settlement_id,
        merchant_id,
        reason,
        description,
        evidence_image_url,
        adjustment_amount,
        status,
        admin_note,
        created_at,
        updated_at
    )
VALUES
    (1, 13, '10000000-0000-0000-0000-000000000001', 'COMMISSION_FEE_MISMATCH', 'Phí chiết khấu kỳ tuần 35 chưa áp dụng đúng chính sách ưu đãi hợp đồng mới cho nhóm món phở đặc biệt. Nhờ Admin kiểm tra và hoàn lại 50.000 đ.', 'https://images.unsplash.com/photo-1554224155-8d04cb21cd6c?w=800', 0.00, 'PENDING', NULL, '2026-08-31 10:20:00', '2026-08-31 10:20:00'),
    (2, 10, '10000000-0000-0000-0000-000000000001', 'OTHER', 'Đề nghị hỗ trợ chi phí mã giảm giá đồng tài trợ trong chiến dịch ngày hội ẩm thực.', 'https://images.unsplash.com/photo-1559526324-4b87b5e36e44?w=800', 30000.00, 'RESOLVED', 'Admin đã kiểm tra chương trình đồng tài trợ và chấp thuận bồi hoàn thêm 30.000 VNĐ.', '2026-08-01 14:00:00', '2026-08-02 09:30:00')
ON DUPLICATE KEY UPDATE
    status = VALUES(status),
    admin_note = VALUES(admin_note);

-- 6. Thêm Lịch Sử Yêu Cầu Rút Tiền (Merchant Payout Requests)
DELETE FROM merchant_payout_requests
WHERE
    id IN (
        '30000000-0000-0000-0000-000000000001',
        '30000000-0000-0000-0000-000000000002',
        '30000000-0000-0000-0000-000000000003'
    );

INSERT INTO
    merchant_payout_requests (
        id,
        merchant_id,
        type,
        amount,
        bank_name,
        bank_account_number,
        status,
        admin_note,
        transfer_proof_url,
        created_at,
        completed_at,
        rejected_at
    )
VALUES
    ('30000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'WITHDRAWAL', 300000, 'Vietcombank', '987654321098', 'COMPLETED', 'Đã thực hiện lệnh chuyển khoản qua ngân hàng Vietcombank cho quán. Mã giao dịch VCB-883921.', 'https://images.unsplash.com/photo-1559526324-4b87b5e36e44?w=800', '2026-08-15 14:00:00', '2026-08-15 16:30:00', NULL),
    ('30000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'WITHDRAWAL', 250000, 'Vietcombank', '987654321098', 'PENDING', NULL, NULL, NOW(), NULL, NULL),
    ('30000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', 'WITHDRAWAL', 150000, 'Vietcombank', '987654321098', 'REJECTED', 'Số tài khoản ngân hàng không trùng khớp với tên pháp nhân đăng ký ban đầu. Quán vui lòng vào Cài đặt tài khoản cập nhật lại.', NULL, '2026-08-10 09:00:00', NULL, '2026-08-10 11:30:00');

-- 7. Thêm Yêu Cầu Đối Tác Thân Thiết (Trusted Partner Requests)
DELETE FROM trusted_partner_requests WHERE id IN (1, 2);

INSERT INTO
    trusted_partner_requests (
        id,
        merchant_id,
        status,
        revenue,
        reject_reason,
        created_at,
        reviewed_at
    )
VALUES
    (1, '10000000-0000-0000-0000-000000000001', 'PENDING', 105000000.00, NULL, '2026-09-01 09:30:00', NULL),
    (2, '10000000-0000-0000-0000-000000000002', 'REJECTED', 85000000.00, 'Doanh thu tháng chưa đạt ngưỡng tối thiểu 100.000.000 VNĐ theo tiêu chuẩn quy định.', '2026-08-15 11:00:00', '2026-08-16 14:00:00')
ON DUPLICATE KEY UPDATE
    status = VALUES(status);