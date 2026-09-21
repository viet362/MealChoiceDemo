package vn.codegyme.meal_choice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.codegyme.meal_choice.entity.Merchant;
import vn.codegyme.meal_choice.entity.MerchantStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, UUID> {

    boolean existsByMerchantEmail(String merchantEmail);

    Optional<Merchant> findByMerchantEmail(String merchantEmail);

    boolean existsByUserId(UUID userId);

    boolean existsByMerchantPhone(String merchantPhone);

    List<Merchant> findAllByOrderByIdDesc();

    List<Merchant> findByMerchantStatusOrderByIdDesc(
            MerchantStatus merchantStatus
    );

    Optional<Merchant> findByUser_Id(UUID userId);

    @Query("""
        SELECT DISTINCT m
        FROM Merchant m
        LEFT JOIN FETCH m.addresses
        WHERE m.id = :id
    """)
    Optional<Merchant> findByIdWithAddresses(@Param("id") UUID id);

    @Query("""
        SELECT DISTINCT m
        FROM Merchant m
        LEFT JOIN FETCH m.addresses
        WHERE m.merchantEmail = :email
    """)
    Optional<Merchant> findByMerchantEmailWithAddresses(
            @Param("email") String email
    );

    @Query("""
        SELECT DISTINCT m
        FROM Merchant m
        LEFT JOIN FETCH m.addresses
        WHERE m.user.id = :userId
    """)
    Optional<Merchant> findByUserIdWithAddresses(
            @Param("userId") UUID userId
    );

    @Query("""
        SELECT DISTINCT m
        FROM Merchant m
        LEFT JOIN FETCH m.addresses
        ORDER BY m.id DESC
        """)
    List<Merchant> findAllWithAddressesOrderByIdDesc();

    @Query("""
        SELECT m.id
        FROM Merchant m
        JOIN m.likedByUsers u
        WHERE u.id = :userId
    """)
    List<UUID> findLikedMerchantIdsByUserId(
            @Param("userId") UUID userId
    );

    @Query(value = "SELECT COUNT(*) FROM merchant_likes WHERE merchant_id = :merchantId", nativeQuery = true)
    long countFollowersByMerchantId(@Param("merchantId") UUID merchantId);

    @Modifying
    @Transactional
    @Query(value = "INSERT IGNORE INTO merchant_likes (merchant_id, user_id) VALUES (:merchantId, :userId)", nativeQuery = true)
    int followMerchant(@Param("merchantId") UUID merchantId, @Param("userId") UUID userId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM merchant_likes WHERE merchant_id = :merchantId AND user_id = :userId", nativeQuery = true)
    int unfollowMerchant(@Param("merchantId") UUID merchantId, @Param("userId") UUID userId);
}