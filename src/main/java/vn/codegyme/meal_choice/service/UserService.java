package vn.codegyme.meal_choice.service;

import vn.codegyme.meal_choice.dto.AddressResponseDTO;
import vn.codegyme.meal_choice.dto.CreateAddressDTO;
import vn.codegyme.meal_choice.dto.UpdateAddressDTO;
import vn.codegyme.meal_choice.dto.UpdateProfileDTO;
import vn.codegyme.meal_choice.dto.UserResponseDTO;

import java.util.List;

public interface UserService {

    /**
     * Lấy thông tin profile của user đang đăng nhập
     */
    UserResponseDTO getCurrentUserProfile();

    /**
     * Cập nhật profile (KHÔNG cho phép sửa email và phoneNumber)
     */
    UserResponseDTO updateProfile(UpdateProfileDTO updateProfileDTO);

    /**
     * Lấy tất cả địa chỉ của user đang đăng nhập
     */
    List<AddressResponseDTO> getAllAddresses();

    /**
     * Lấy chi tiết địa chỉ theo ID
     */
    AddressResponseDTO getAddressById(Long addressId);

    /**
     * Thêm địa chỉ mới
     */
    UserResponseDTO addAddress(CreateAddressDTO createAddressDTO);

    /**
     * Cập nhật địa chỉ đã có (KHÔNG sửa thông tin liên hệ contactName & contactPhone)
     */
    UserResponseDTO updateAddress(Long addressId, UpdateAddressDTO updateAddressDTO);

    /**
     * Xóa địa chỉ
     */
    UserResponseDTO deleteAddress(Long addressId);

    /**
     * Đặt địa chỉ mặc định
     */
    UserResponseDTO setDefaultAddress(Long addressId);
}