package vn.codegyme.meal_choice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegyme.meal_choice.dto.merchant.MerchantAddressResponse;
import vn.codegyme.meal_choice.dto.merchant.MerchantRegisterRequest;
import vn.codegyme.meal_choice.dto.merchant.MerchantResponse;
import vn.codegyme.meal_choice.dto.merchant.MerchantUpdateRequest;
import vn.codegyme.meal_choice.entity.Merchant;
import vn.codegyme.meal_choice.entity.MerchantAddress;
import vn.codegyme.meal_choice.entity.MerchantStatus;
import vn.codegyme.meal_choice.entity.User;
import vn.codegyme.meal_choice.repository.MerchantAddressRepository;
import vn.codegyme.meal_choice.repository.MerchantRepository;
import vn.codegyme.meal_choice.repository.UserRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchantService {

    private final UserRepository userRepository;
    private final MerchantRepository merchantRepository;
    private final MerchantAddressRepository merchantAddressRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void updateMerchantProfile(UUID merchantId, MerchantUpdateRequest request) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Merchant"));

        if (request.getMerchantRestaurantName() != null
                && !request.getMerchantRestaurantName().trim().isEmpty()) {
            merchant.setMerchantRestaurantName(request.getMerchantRestaurantName().trim());
        }

        String bankName = request.getBankName() != null ? request.getBankName().trim() : null;
        String bankAccountNumber = request.getBankAccountNumber() != null ? request.getBankAccountNumber().trim() : null;

        boolean hasBankName = bankName != null && !bankName.isEmpty();
        boolean hasBankAccount = bankAccountNumber != null && !bankAccountNumber.isEmpty();

        // Ràng buộc: Hoặc cả 2 cùng có, hoặc cả 2 cùng trống
        if ((hasBankName && !hasBankAccount) || (!hasBankName && hasBankAccount)) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ cả Tên ngân hàng và Số tài khoản hoặc để trống cả hai.");
        }

        merchant.setBankName(hasBankName ? bankName : null);
        merchant.setBankAccountNumber(hasBankAccount ? bankAccountNumber : null);

        merchantRepository.save(merchant);
    }

    @Transactional
    public void registerMerchant(
            MerchantRegisterRequest request,
            String userEmail) {

        // 1. TÌM USER

        User user = userRepository
                .findByEmail(userEmail)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy tài khoản người dùng"
                        )
                );

        // 2. KIỂM TRA MẬT KHẨU NHẬP LẠI
        if (!request.getPassword()
                .equals(request.getConfirmPassword())) {

            throw new RuntimeException(
                    "Mật khẩu nhập lại không đúng"
            );
        }

        // 3. KIỂM TRA MẬT KHẨU TÀI KHOẢN
        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new RuntimeException(
                    "Mật khẩu tài khoản không đúng"
            );
        }

        // 4. KIỂM TRA MERCHANT CŨ
        Merchant merchant = merchantRepository
                .findByUser_Id(user.getId())
                .orElse(null);

        /*
         * Khai báo MerchantAddress ở ngoài
         * để có thể sử dụng cho cả hai trường hợp:
         *
         * - Đăng ký lại
         * - Đăng ký lần đầu
         */
        MerchantAddress merchantAddress;

        // 5. USER ĐÃ TỪNG ĐĂNG KÝ MERCHANT
        if (merchant != null) {
            // Chỉ cho đăng ký lại nếu trước đó bị từ chối
            if (merchant.getMerchantStatus()
                    != MerchantStatus.REJECTED) {
                throw new RuntimeException(
                        "Tài khoản này đã đăng ký Merchant"
                );
            }

            // KIỂM TRA EMAIL TRÙNG
            if (!merchant.getMerchantEmail()
                    .equals(request.getMerchantEmail())
                    && merchantRepository
                    .existsByMerchantEmail(
                            request.getMerchantEmail()
                    )) {

                throw new RuntimeException(
                        "Email Merchant đã tồn tại"
                );
            }

            // KIỂM TRA PHONE TRÙNG
            if (!merchant.getMerchantPhone()
                    .equals(request.getMerchantPhone())
                    && merchantRepository
                    .existsByMerchantPhone(
                            request.getMerchantPhone()
                    )) {

                throw new RuntimeException(
                        "Số điện thoại đã tồn tại"
                );
            }  

            // CẬP NHẬT MERCHANT CŨ
            merchant.setMerchantRestaurantName(
                    request.getMerchantRestaurantName()
            );

            merchant.setMerchantEmail(
                    request.getMerchantEmail()
            );

            merchant.setMerchantPhone(
                    request.getMerchantPhone()
            );

            merchant.setMerchantStatus(
                    MerchantStatus.PENDING
            );

            // Xóa lý do từ chối cũ
            merchant.setRejectReason(null);

            merchantRepository.save(merchant);

            // LẤY ĐỊA CHỈ CŨ
            List<MerchantAddress> addresses =
                    merchantAddressRepository
                            .findByMerchantId(
                                    merchant.getId()
                            );

            if (!addresses.isEmpty()) {

                merchantAddress =
                        addresses.get(0);

            } else {

                merchantAddress =
                        new MerchantAddress();

                merchantAddress.setMerchant(
                        merchant
                );
            }

        }

        // 6. ĐĂNG KÝ MERCHANT LẦN ĐẦU
        else {

            // Kiểm tra email
            if (merchantRepository
                    .existsByMerchantEmail(
                            request.getMerchantEmail()
                    )) {

                throw new RuntimeException(
                        "Email Merchant đã tồn tại"
                );
            }

            // Kiểm tra phone
            if (merchantRepository
                    .existsByMerchantPhone(
                            request.getMerchantPhone()
                    )) {

                throw new RuntimeException(
                        "Số điện thoại đã tồn tại"
                );
            }

            // TẠO MERCHANT
            merchant = new Merchant();

            merchant.setMerchantRestaurantName(
                    request.getMerchantRestaurantName()
            );

            merchant.setMerchantEmail(
                    request.getMerchantEmail()
            );

            merchant.setMerchantPhone(
                    request.getMerchantPhone()
            );

            merchant.setUser(
                    user
            );

            merchant.setMerchantStatus(
                    MerchantStatus.PENDING
            );

            merchantRepository.save(
                    merchant
            );

            // TẠO ĐỊA CHỈ MỚI
            merchantAddress =
                    new MerchantAddress();

            merchantAddress.setMerchant(
                    merchant
            );
        }


        // 7. CẬP NHẬT THÔNG TIN ĐỊA CHỈ
        merchantAddress.setMerchantAddress(
                request.getMerchantAddress()
        );

        merchantAddress.setProvinceCode(
                request.getProvinceCode()
        );

        merchantAddress.setDistrictCode(
                request.getDistrictCode()
        );

        merchantAddress.setWardCode(
                request.getWardCode()
        );

        merchantAddress.setDefault(
                true
        );

        merchantAddressRepository.save(
                merchantAddress
        );

        // 8. GỬI EMAIL
        emailService.sendMerchantRegisterEmail(
                request.getMerchantEmail(),
                request.getMerchantRestaurantName()
        );
    }

        @Transactional
        
        public void updateMerchantProfile(UUID merchantId, String merchantRestaurantName) {
             Merchant merchant = merchantRepository.findById(merchantId)
                  .orElseThrow(() -> new RuntimeException("Không tìm thấy Merchant"));

              merchant.setMerchantRestaurantName(merchantRestaurantName);
              merchantRepository.save(merchant);
}

        @Transactional
        public void updateMerchant(
            UUID merchantId,
            MerchantUpdateRequest request) {

        Merchant merchant = merchantRepository
                .findById(merchantId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy Merchant"));

        merchant.setMerchantRestaurantName(
                request.getMerchantRestaurantName());

        merchantRepository.save(merchant);

        List<MerchantAddress> addresses =
                merchantAddressRepository
                        .findByMerchantId(merchantId);

        MerchantAddress address;

        if (!addresses.isEmpty()) {
            address = addresses.get(0);
        } else {
            address = new MerchantAddress();
            address.setMerchant(merchant);
        }

        address.setMerchantAddress(
                request.getMerchantAddress());

        address.setProvinceCode(
                request.getProvinceCode());

        address.setDistrictCode(
                request.getDistrictCode());

        address.setWardCode(
                request.getWardCode());

        address.setMerchantOpenTime(
                request.getMerchantOpenTime());

        address.setMerchantCloseTime(
                request.getMerchantCloseTime());

        merchantAddressRepository.save(address);
    }

    public MerchantResponse getMerchant(
            UUID merchantId) {

        Merchant merchant = merchantRepository
                .findById(merchantId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy Merchant"));

        MerchantResponse response =
                new MerchantResponse();

        response.setId(merchant.getId());

        response.setMerchantRestaurantName(
                merchant.getMerchantRestaurantName());

        response.setMerchantEmail(
                merchant.getMerchantEmail());

        response.setMerchantPhone(
                merchant.getMerchantPhone());

        response.setMerchantStatus(
                merchant.getMerchantStatus());

        response.setBankName(
                merchant.getBankName());

        response.setBankAccountNumber(
                merchant.getBankAccountNumber());

        List<MerchantAddressResponse> addressResponses =
                merchantAddressRepository
                        .findByMerchantId(merchantId)
                        .stream()
                        .map(addr -> {

                            MerchantAddressResponse res =
                                    new MerchantAddressResponse();

                            res.setId(addr.getId());

                            res.setProvinceCode(
                                    addr.getProvinceCode());

                            res.setDistrictCode(
                                    addr.getDistrictCode());

                            res.setWardCode(
                                    addr.getWardCode());

                            res.setMerchantAddress(
                                    addr.getMerchantAddress());

                            res.setMerchantOpenTime(
                                    addr.getMerchantOpenTime());

                            res.setMerchantCloseTime(
                                    addr.getMerchantCloseTime());

                            res.setDefault(
                                    addr.isDefault());

                            return res;
                        })
                        .toList();

        response.setAddresses(addressResponses);
        return response;
    }
    
}