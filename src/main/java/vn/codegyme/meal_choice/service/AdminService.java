package vn.codegyme.meal_choice.service;

import vn.codegyme.meal_choice.entity.DeliveryPartner;
import vn.codegyme.meal_choice.entity.DeliveryPartnerStatus;
import vn.codegyme.meal_choice.entity.Merchant;
import vn.codegyme.meal_choice.entity.MerchantStatus;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface AdminService {

    // Xem danh sách merchant
    List<Merchant> getAllMerchants();

    // Lọc theo trạng thái
    List<Merchant> getMerchantsByStatus(MerchantStatus status);

    List<DeliveryPartner> getAllDeliveryPartners();

    List<DeliveryPartner> getDeliveryPartnersByStatus(
            DeliveryPartnerStatus status
    );

    DeliveryPartner getDeliveryPartnerById(UUID id);

    // Xem chi tiết
    Merchant getMerchantById(UUID id);

    // Duyệt merchant
    void approveMerchant(UUID id);

    // Từ chối merchant
    void rejectMerchant(UUID id, String reason);

    // Khóa/ lý do khóa/ mở khóa
    void toggleMerchantLockStatus(
            UUID id,
            String lockReason
    );

    // Duyệt đối tác thân thiết
    void approveTrustedPartner(UUID id);

    // Từ chối đăng ký đối tác thân thiết
    void rejectTrustedPartner(UUID id, String reason);

    // Bỏ đối tác thân thiết
    void removeTrustedPartner(UUID id);

    // Tạo đối tác vận chuyển
    void createDeliveryPartner(
            DeliveryPartner partner
    );

    void updateDeliveryPartner(
            UUID id,
            DeliveryPartner partner
    );

    void toggleDeliveryPartnerLock(
            UUID id,
            String lockReason
    );

    // lấy danh sách merchant đang có yêu cầu đối tác thân thiết
    Set<UUID> getPendingTrustedPartnerMerchantIds();
}