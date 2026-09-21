package vn.codegyme.meal_choice.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.codegyme.meal_choice.entity.DeliveryPartner;
import vn.codegyme.meal_choice.entity.DeliveryPartnerStatus;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeliveryPartnerRepository
        extends JpaRepository<DeliveryPartner, UUID> {

    List<DeliveryPartner> findAllByOrderByCreatedAtDesc();

    List<DeliveryPartner>
    findByStatusOrderByCreatedAtDesc(
            DeliveryPartnerStatus status
    );

    List<DeliveryPartner>
    findByStatus(
            DeliveryPartnerStatus status
    );

    boolean existsByPartnerCode(
            String partnerCode
    );
}
