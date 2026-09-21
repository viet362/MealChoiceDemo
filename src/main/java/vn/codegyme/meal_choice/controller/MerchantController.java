package vn.codegyme.meal_choice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import vn.codegyme.meal_choice.dto.merchant.MerchantAddressRequest;
import vn.codegyme.meal_choice.dto.merchant.MerchantAddressResponse;
import vn.codegyme.meal_choice.dto.merchant.MerchantRegisterRequest;
import vn.codegyme.meal_choice.dto.merchant.MerchantResponse;
import vn.codegyme.meal_choice.dto.merchant.MerchantUpdateRequest;
import vn.codegyme.meal_choice.entity.Merchant;
import vn.codegyme.meal_choice.entity.User;
import vn.codegyme.meal_choice.repository.MerchantRepository;
import vn.codegyme.meal_choice.repository.UserRepository;
import vn.codegyme.meal_choice.service.MerchantAddressService;
import vn.codegyme.meal_choice.service.MerchantService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
public class MerchantController {

        private final MerchantService merchantService;
        private final MerchantAddressService merchantAddressService;
        private final MerchantRepository merchantRepository;
        private final UserRepository userRepository;

        // Đăng ký Merchant
        @PostMapping("/register")
        public ResponseEntity<String> registerMerchant(
                @Valid @RequestBody MerchantRegisterRequest request,
                Authentication authentication) {

                String userEmail = authentication.getName();

                merchantService.registerMerchant(
                        request,
                        userEmail);

                return ResponseEntity.ok(
                        "Đăng ký Merchant thành công, đang chờ Admin phê duyệt");
        }

        // Lấy trạng thái Merchant của User hiện tại
        @GetMapping("/my-status")
        public ResponseEntity<Map<String, Object>> getMyMerchantStatus(
                Authentication authentication) {

                String userEmail = authentication.getName();

                User user = userRepository.findByEmail(userEmail)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Không tìm thấy tài khoản"));

                Map<String, Object> response = new HashMap<>();

                Merchant merchant = merchantRepository
                        .findByUser_Id(user.getId())
                        .orElse(null);

                if (merchant == null) {
                        response.put("registered", false);
                        response.put("status", null);
                } else {
                        response.put("registered", true);
                        response.put(
                                "status",
                                merchant.getMerchantStatus().name());
                }

                return ResponseEntity.ok(response);
        }

        // Lấy thông tin Merchant của User đang đăng nhập
        @GetMapping("/my-profile")
        public ResponseEntity<MerchantResponse> getMyMerchantProfile(
                Authentication authentication) {

                String userEmail = authentication.getName();

                User user = userRepository.findByEmail(userEmail)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Không tìm thấy tài khoản"));

                Merchant merchant = merchantRepository
                        .findByUser_Id(user.getId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Tài khoản chưa đăng ký Merchant"));

                MerchantResponse response =
                        merchantService.getMerchant(
                                merchant.getId());

                return ResponseEntity.ok(response);
        }

        // Thêm địa chỉ Merchant
        @PostMapping("/{merchantId}/addresses")
        public ResponseEntity<String> createAddress(
                @PathVariable("merchantId") UUID merchantId,
                @Valid @RequestBody MerchantAddressRequest request) {

                merchantAddressService.createAddress(
                        merchantId,
                        request);

                return ResponseEntity.ok(
                        "Thêm địa chỉ thành công");
        }

        // Lấy danh sách địa chỉ Merchant
        @GetMapping("/{merchantId}/addresses")
        public ResponseEntity<List<MerchantAddressResponse>> getAddresses(
                @PathVariable("merchantId") UUID merchantId) {

                List<MerchantAddressResponse> addresses =
                        merchantAddressService.getAddresses(
                                merchantId);

                return ResponseEntity.ok(addresses);
        }

        // Cập nhật thông tin Merchant
        @PutMapping("/{merchantId}/profile")
        public ResponseEntity<String> updateMerchantProfile(
                @PathVariable("merchantId") UUID merchantId,
                @Validated(MerchantUpdateRequest.ProfileUpdate.class)
                @RequestBody MerchantUpdateRequest request) {

                merchantService.updateMerchantProfile(
                        merchantId,
                        request);

                return ResponseEntity.ok(
                        "Cập nhật thông tin Merchant thành công");
        }

        // Cập nhật địa chỉ Merchant
        @PutMapping("/{merchantId}/addresses/{addressId}")
        public ResponseEntity<String> updateAddress(
                @PathVariable("merchantId") UUID merchantId,
                @PathVariable("addressId") UUID addressId,
                @Valid @RequestBody MerchantAddressRequest request) {

                merchantAddressService.updateAddress(
                        merchantId,
                        addressId,
                        request);

                return ResponseEntity.ok(
                        "Cập nhật địa chỉ thành công");
        }

        // Xóa địa chỉ Merchant
        @DeleteMapping("/{merchantId}/addresses/{addressId}")
        public ResponseEntity<String> deleteAddress(
                @PathVariable("merchantId") UUID merchantId,
                @PathVariable("addressId") UUID addressId) {

                merchantAddressService.deleteAddress(
                        merchantId,
                        addressId);

                return ResponseEntity.ok(
                        "Xóa địa chỉ thành công");
        }

        // Lấy thông tin Merchant theo ID
        @GetMapping("/{merchantId}")
        public ResponseEntity<MerchantResponse> getMerchant(
                @PathVariable("merchantId") UUID merchantId) {

                MerchantResponse response =
                        merchantService.getMerchant(
                                merchantId);

                return ResponseEntity.ok(response);
        }
}