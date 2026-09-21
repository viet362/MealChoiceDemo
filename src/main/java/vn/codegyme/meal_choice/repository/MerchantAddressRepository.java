package vn.codegyme.meal_choice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.codegyme.meal_choice.entity.MerchantAddress;

import java.util.List;
import java.util.UUID;

@Repository
public interface MerchantAddressRepository extends JpaRepository<MerchantAddress, UUID> {

    List<MerchantAddress> findByMerchantId(UUID merchantId);
}