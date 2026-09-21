package vn.codegyme.meal_choice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegyme.meal_choice.dto.merchant.MerchantAddressRequest;
import vn.codegyme.meal_choice.dto.merchant.MerchantAddressResponse;
import vn.codegyme.meal_choice.entity.Merchant;
import vn.codegyme.meal_choice.entity.MerchantAddress;
import vn.codegyme.meal_choice.repository.FoodRepository;
import vn.codegyme.meal_choice.repository.MerchantAddressRepository;
import vn.codegyme.meal_choice.repository.MerchantRepository;

import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class MerchantAddressService {
    private final FoodRepository foodRepository;

    private final MerchantRepository merchantRepository;
    private final MerchantAddressRepository merchantAddressRepository;

    @Transactional
    public void createAddress(
            UUID merchantId,
            MerchantAddressRequest request) {

        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy Merchant"));

        MerchantAddress address = new MerchantAddress();

        address.setMerchant(merchant);

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

        address.setDefault(
                request.isDefault());

        merchantAddressRepository.save(address);
    }

    public List<MerchantAddressResponse> getAddresses(
            UUID merchantId) {

        if (!merchantRepository.existsById(merchantId)) {
            throw new RuntimeException(
                    "Không tìm thấy Merchant");
        }

        List<MerchantAddress> addresses =
                merchantAddressRepository
                        .findByMerchantId(merchantId);

        return addresses.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public void updateAddress(
            UUID merchantId,
            UUID addressId,
            MerchantAddressRequest request) {

        MerchantAddress address =
                merchantAddressRepository
                        .findById(addressId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Không tìm thấy địa chỉ"));

        if (!address.getMerchant()
                .getId()
                .equals(merchantId)) {

            throw new RuntimeException(
                    "Địa chỉ không thuộc Merchant này");
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

        address.setDefault(
                request.isDefault());

        merchantAddressRepository.save(address);
    }

    @Transactional
    public void deleteAddress(UUID merchantId, UUID addressId) {
        MerchantAddress address = merchantAddressRepository
                .findById(addressId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

        if (!address.getMerchant().getId().equals(merchantId)) {
            throw new RuntimeException("Địa chỉ không thuộc Merchant này");
        }

        if (foodRepository.existsByMerchantAddress_Id(addressId)) {
            throw new RuntimeException(
                    "Không thể xóa địa chỉ này vì đang có món ăn sử dụng."
            );
        }

        merchantAddressRepository.delete(address);
    }

    // Chuyển MerchantAddress Entity thành MerchantAddressResponse
    private MerchantAddressResponse mapToResponse(
            MerchantAddress address) {

        MerchantAddressResponse response =
                new MerchantAddressResponse();

        response.setId(address.getId());

        response.setProvinceCode(
                address.getProvinceCode());

        response.setDistrictCode(
                address.getDistrictCode());

        response.setWardCode(
                address.getWardCode());

        response.setMerchantAddress(
                address.getMerchantAddress());

        response.setMerchantOpenTime(
                address.getMerchantOpenTime());

        response.setMerchantCloseTime(
                address.getMerchantCloseTime());

        response.setDefault(
                address.isDefault());

        return response;
    }
}