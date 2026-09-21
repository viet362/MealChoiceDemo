package vn.codegyme.meal_choice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.codegyme.meal_choice.entity.Order;
import vn.codegyme.meal_choice.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"orderItems", "merchant", "user", "deliveryPartner"})
    List<Order> findByMerchant_IdOrderByIdDesc(UUID merchantId);

    @EntityGraph(attributePaths = {"orderItems", "merchant", "user", "deliveryPartner"})
    List<Order> findByMerchant_IdAndStatusOrderByIdDesc(UUID merchantId, OrderStatus status);

    @EntityGraph(attributePaths = {"orderItems", "merchant", "user", "deliveryPartner"})
    Page<Order> findByMerchant_IdOrderByIdDesc(UUID merchantId, Pageable pageable);

    @EntityGraph(attributePaths = {"orderItems", "merchant", "user", "deliveryPartner"})
    Page<Order> findByMerchant_IdAndStatusOrderByIdDesc(UUID merchantId, OrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"orderItems", "merchant", "user", "deliveryPartner"})
    Optional<Order> findByIdAndMerchant_Id(Long id, UUID merchantId);

    @EntityGraph(attributePaths = {"orderItems", "merchant", "user", "deliveryPartner"})
    Optional<Order> findByOrderCode(String orderCode);

    @EntityGraph(attributePaths = {"orderItems", "merchant", "user", "deliveryPartner"})
    List<Order> findByUser_IdOrderByIdDesc(UUID userId);

    @EntityGraph(attributePaths = {"orderItems", "merchant", "user", "deliveryPartner"})
    Page<Order> findByUser_IdOrderByIdDesc(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"orderItems", "merchant", "user", "deliveryPartner"})
    Page<Order> findByUser_IdAndStatusOrderByIdDesc(UUID userId, OrderStatus status, Pageable pageable);

    long countByMerchant_IdAndStatus(UUID merchantId, OrderStatus status);

    // Tìm kiếm đơn hàng theo mã đơn, tên khách hàng hoặc số điện thoại
    @EntityGraph(attributePaths = {"orderItems", "merchant", "user", "deliveryPartner"})
    @Query("""
        SELECT o FROM Order o
        WHERE o.merchant.id = :merchantId
        AND (
            LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(o.contactName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR o.contactPhone LIKE CONCAT('%', :keyword, '%')
        )
        ORDER BY o.id DESC
        """)
    Page<Order> searchOrders(
            @Param("merchantId") UUID merchantId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // Tìm kiếm đơn hàng theo từ khóa và trạng thái
    @EntityGraph(attributePaths = {"orderItems", "merchant", "user", "deliveryPartner"})
    @Query("""
        SELECT o FROM Order o
        WHERE o.merchant.id = :merchantId
        AND o.status = :status
        AND (
            LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(o.contactName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR o.contactPhone LIKE CONCAT('%', :keyword, '%')
        )
        ORDER BY o.id DESC
        """)
    Page<Order> searchOrdersByStatus(
            @Param("merchantId") UUID merchantId,
            @Param("status") OrderStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    long countByUser_IdAndStatus(UUID userId, OrderStatus status);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.food LEFT JOIN FETCH o.deliveryPartner WHERE o.id = :id AND o.merchant.id = :merchantId")
    Optional<Order> findByIdAndMerchantIdWithItems(@Param("id") Long id, @Param("merchantId") UUID merchantId);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.food LEFT JOIN FETCH o.deliveryPartner WHERE o.orderCode = :orderCode")
    Optional<Order> findByOrderCodeWithItems(@Param("orderCode") String orderCode);

    // ==================== TRUY VẤN THEO KHÁCH HÀNG ====================

    /**
     * Chi tiết một đơn hàng CỦA CHÍNH khách hàng đang đăng nhập.
     * Điều kiện user.id nằm ngay trong câu truy vấn nên không thể xem đơn của người khác.
     */
    @Query("""
            SELECT o FROM Order o
            LEFT JOIN FETCH o.orderItems oi
            LEFT JOIN FETCH oi.food
            LEFT JOIN FETCH o.merchant
            LEFT JOIN FETCH o.deliveryPartner
            WHERE o.id = :id AND o.user.id = :userId
            """)
    Optional<Order> findByIdAndUserIdWithItems(@Param("id") Long id, @Param("userId") UUID userId);

    /**
     * Chi tiết theo mã đơn, có kiểm tra chủ sở hữu.
     */
    @Query("""
            SELECT o FROM Order o
            LEFT JOIN FETCH o.orderItems oi
            LEFT JOIN FETCH oi.food
            LEFT JOIN FETCH o.merchant
            LEFT JOIN FETCH o.deliveryPartner
            WHERE o.orderCode = :orderCode AND o.user.id = :userId
            """)
    Optional<Order> findByOrderCodeAndUserIdWithItems(@Param("orderCode") String orderCode,
                                                      @Param("userId") UUID userId);

    @EntityGraph(attributePaths = {"orderItems", "merchant", "user", "deliveryPartner"})
    Optional<Order> findByIdAndUser_Id(Long id, UUID userId);

    // 1. THỐNG KÊ DOANH THU
    @Query(value = """
        SELECT 
            CONCAT('Tháng ', MONTH(created_at), '/', YEAR(created_at)) AS period,
            COUNT(id) AS totalOrders,
            COALESCE(SUM(total_amount), 0) AS totalRevenue
        FROM orders
        WHERE CAST(merchant_id AS CHAR) = CAST(:merchantId AS CHAR) 
          AND status IN ('PREPARING', 'DELIVERING', 'COMPLETED')
        GROUP BY period
        """, nativeQuery = true)
    List<Object[]> findRevenueStatsByMonth(@Param("merchantId") String merchantId);

    @Query(value = """
        SELECT 
            CONCAT('Tuần ', WEEK(created_at), '/', YEAR(created_at)) AS period,
            COUNT(id) AS totalOrders,
            COALESCE(SUM(total_amount), 0) AS totalRevenue
        FROM orders
        WHERE CAST(merchant_id AS CHAR) = CAST(:merchantId AS CHAR) 
          AND status IN ('PREPARING', 'DELIVERING', 'COMPLETED')
        GROUP BY period
        """, nativeQuery = true)
    List<Object[]> findRevenueStatsByWeek(@Param("merchantId") String merchantId);

    @Query(value = """
        SELECT 
            CONCAT('Quý ', QUARTER(created_at), '/', YEAR(created_at)) AS period,
            COUNT(id) AS totalOrders,
            COALESCE(SUM(total_amount), 0) AS totalRevenue
        FROM orders
        WHERE CAST(merchant_id AS CHAR) = CAST(:merchantId AS CHAR) 
          AND status IN ('PREPARING', 'DELIVERING', 'COMPLETED')
        GROUP BY period
        """, nativeQuery = true)
    List<Object[]> findRevenueStatsByQuarter(@Param("merchantId") String merchantId);

    // 2. THỐNG KÊ KHÁCH HÀNG (Khách hàng thân thiết)
    @Query(value = """
        SELECT 
            contact_name AS customerName,
            contact_phone AS customerPhone,
            COUNT(id) AS totalOrders,
            COALESCE(SUM(total_amount), 0) AS totalSpent
        FROM orders
        WHERE CAST(merchant_id AS CHAR) = CAST(:merchantId AS CHAR) 
          AND status IN ('PREPARING', 'DELIVERING', 'COMPLETED')
        GROUP BY contact_name, contact_phone
        ORDER BY totalOrders DESC, totalSpent DESC
        """, nativeQuery = true)
    List<Object[]> findCustomerStatsByMerchant(@Param("merchantId") String merchantId);

    // 3. THỐNG KÊ COUPON
    @Query(value = """
        SELECT 
            COUNT(id) AS totalOrdersWithDiscount,
            COALESCE(SUM(discount_amount), 0) AS totalDiscountAmount,
            COALESCE(SUM(total_amount), 0) AS totalRevenue
        FROM orders
        WHERE CAST(merchant_id AS CHAR) = CAST(:merchantId AS CHAR) 
          AND status IN ('PREPARING', 'DELIVERING', 'COMPLETED')
        """, nativeQuery = true)
    List<Object[]> findCouponStatsByMerchant(@Param("merchantId") String merchantId);

    // 4. THỐNG KÊ MÓN ĂN (Top món bán chạy)
    @Query(value = """
        SELECT 
            TRIM(oi.food_name) AS foodName,
            COALESCE(SUM(oi.quantity), 0) AS totalQuantity,
            COALESCE(SUM(oi.subtotal), 0) AS totalRevenue
        FROM order_items oi
        JOIN orders o ON oi.order_id = o.id
        WHERE CAST(o.merchant_id AS CHAR) = CAST(:merchantId AS CHAR) 
          AND o.status IN ('PREPARING', 'DELIVERING', 'COMPLETED')
        GROUP BY TRIM(oi.food_name)
        ORDER BY totalQuantity DESC
        """, nativeQuery = true)
    List<Object[]> findFoodStatsByMerchant(@Param("merchantId") String merchantId);

    // đăng ký thân thiết yêu cầu tính doanh thu tháng hiện tại
    @Query("""
        SELECT COALESCE(SUM(o.totalAmount), 0)
        FROM Order o
        WHERE o.merchant.id = :merchantId
          AND o.status = :status
          AND o.createdAt >= :startDate
          AND o.createdAt < :endDate
        """)
    BigDecimal calculateRevenue(
            @Param("merchantId") UUID merchantId,
            @Param("status") OrderStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Tổng doanh thu tích lũy của Merchant từ các đơn đã dù hoàn thành hay chưa hoàn thành
    @Query("""
    SELECT COALESCE(SUM(o.totalAmount), 0)
    FROM Order o
    WHERE o.merchant.id = :merchantId
    """)
    BigDecimal calculateTotalRevenue(
            @Param("merchantId") UUID merchantId
    );

    // Lấy danh sách đơn hoàn thành trong kỳ đối soát (theo thời điểm hoàn thành đơn)
    @Query("""
        SELECT o FROM Order o
        WHERE o.merchant.id = :merchantId
          AND o.status = vn.codegyme.meal_choice.entity.OrderStatus.COMPLETED
          AND COALESCE(o.completedAt, o.updatedAt, o.createdAt) >= :startDate
          AND COALESCE(o.completedAt, o.updatedAt, o.createdAt) < :endDate
        ORDER BY COALESCE(o.completedAt, o.updatedAt, o.createdAt) DESC
        """)
    List<Order> findCompletedOrdersInPeriod(
            @Param("merchantId") UUID merchantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}