package vn.codegyme.meal_choice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.codegyme.meal_choice.entity.Cart;
import vn.codegyme.meal_choice.entity.Food;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUser_Id(UUID userId);

    /**
     * Lấy giỏ hàng kèm các dòng món và Food tương ứng.
     *
     * KHÔNG fetch f.images ở đây. Cart.items và Food.images đều là List (bag),
     * Hibernate không cho fetch join hai bag cùng lúc và sẽ ném
     * MultipleBagFetchException. Ảnh được nạp riêng bằng findFoodsWithImagesByIds.
     */
    @Query("""
            SELECT DISTINCT c
            FROM Cart c
            LEFT JOIN FETCH c.items i
            LEFT JOIN FETCH i.food f
            LEFT JOIN FETCH f.merchant
            WHERE c.user.id = :userId
            """)
    Optional<Cart> findByUserIdWithItems(@Param("userId") UUID userId);

    /**
     * Nạp sẵn ảnh cho danh sách món trong giỏ.
     *
     * Gọi trong cùng một transaction với findByUserIdWithItems: các entity Food
     * trả về ở đây chính là những instance đang nằm trong persistence context,
     * nên sau lời gọi này food.getImages() đã sẵn sàng, không phát sinh N+1.
     */
    @Query("""
            SELECT DISTINCT f
            FROM Food f
            LEFT JOIN FETCH f.images
            WHERE f.id IN :foodIds
            """)
    List<Food> findFoodsWithImagesByIds(@Param("foodIds") Collection<Long> foodIds);

    boolean existsByUser_Id(UUID userId);
}