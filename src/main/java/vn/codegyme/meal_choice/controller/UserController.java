package vn.codegyme.meal_choice.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.codegyme.meal_choice.dto.AddressResponseDTO;
import vn.codegyme.meal_choice.dto.CreateAddressDTO;
import vn.codegyme.meal_choice.dto.UpdateAddressDTO;
import vn.codegyme.meal_choice.dto.UpdateProfileDTO;
import vn.codegyme.meal_choice.dto.UserResponseDTO;
import vn.codegyme.meal_choice.service.UserService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * GET /api/user/profile - Lấy thông tin profile
     */
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile() {
        UserResponseDTO userResponseDTO = userService.getCurrentUserProfile();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Lấy thông tin profile thành công");
        response.put("data", userResponseDTO);

        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /api/user/profile - Cập nhật partial profile
     * KHÔNG cho phép sửa email và phoneNumber
     */
    @PatchMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(@Valid @RequestBody UpdateProfileDTO updateProfileDTO) {
        UserResponseDTO updatedUser = userService.updateProfile(updateProfileDTO);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Cập nhật profile thành công");
        response.put("data", updatedUser);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/user/address - Lấy tất cả danh sách địa chỉ của người dùng
     */
    @GetMapping("/address")
    public ResponseEntity<Map<String, Object>> getAllAddresses() {
        List<AddressResponseDTO> addresses = userService.getAllAddresses();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Lấy danh sách địa chỉ thành công");
        response.put("data", addresses);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/user/address/{id} - Lấy chi tiết 1 địa chỉ theo ID
     */
    @GetMapping("/address/{id}")
    public ResponseEntity<Map<String, Object>> getAddressById(@PathVariable("id") Long id) {
        AddressResponseDTO address = userService.getAddressById(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Lấy chi tiết địa chỉ thành công");
        response.put("data", address);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/user/address - Thêm địa chỉ mới
     */
    @PostMapping("/address")
    public ResponseEntity<Map<String, Object>> addAddress(@Valid @RequestBody CreateAddressDTO createAddressDTO) {
        UserResponseDTO updatedUser = userService.addAddress(createAddressDTO);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Thêm địa chỉ thành công");
        response.put("data", updatedUser);

        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /api/user/address/{id} - Sửa địa chỉ đã có (Partial update)
     */
    @PatchMapping("/address/{id}")
    public ResponseEntity<Map<String, Object>> updateAddress(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateAddressDTO updateAddressDTO) {

        UserResponseDTO updatedUser = userService.updateAddress(id, updateAddressDTO);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Cập nhật địa chỉ thành công");
        response.put("data", updatedUser);

        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/user/address/{id} - Xóa địa chỉ
     */
    @DeleteMapping("/address/{id}")
    public ResponseEntity<Map<String, Object>> deleteAddress(@PathVariable("id") Long id) {
        UserResponseDTO updatedUser = userService.deleteAddress(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Xóa địa chỉ thành công");
        response.put("data", updatedUser);

        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /api/user/address/{id}/set-default - Đặt địa chỉ mặc định
     */
    @PatchMapping("/address/{id}/set-default")
    public ResponseEntity<Map<String, Object>> setDefaultAddress(@PathVariable("id") Long id) {
        UserResponseDTO updatedUser = userService.setDefaultAddress(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Đặt địa chỉ mặc định thành công");
        response.put("data", updatedUser);

        return ResponseEntity.ok(response);
    }
}
