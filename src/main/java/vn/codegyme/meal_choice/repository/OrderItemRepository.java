package vn.codegyme.meal_choice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.codegyme.meal_choice.dto.stat.FoodStatDTO;
import vn.codegyme.meal_choice.entity.OrderItem;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrder_Id(Long orderId);

    // Thống kê đơn hàng theo món ăn (Đã mở rộng tính cả đơn PREPARING, DELIVERING, COMPLETED)
    @Query("""
    SELECT new vn.codegyme.meal_choice.dto.stat.FoodStatDTO(
        MIN(f.id), TRIM(oi.foodName), SUM(oi.quantity), SUM(oi.subtotal)
    )
    FROM OrderItem oi JOIN oi.food f JOIN oi.order o
    WHERE o.merchant.id = :merchantId 
      AND o.status IN (
          vn.codegyme.meal_choice.entity.OrderStatus.PREPARING,
          vn.codegyme.meal_choice.entity.OrderStatus.DELIVERING,
          vn.codegyme.meal_choice.entity.OrderStatus.COMPLETED
      )
    GROUP BY TRIM(oi.foodName)
    ORDER BY SUM(oi.quantity) DESC
    """)
    List<FoodStatDTO> findFoodStatsByMerchant(@Param("merchantId") UUID merchantId);
}
