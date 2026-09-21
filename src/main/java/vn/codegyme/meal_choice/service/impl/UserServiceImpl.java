package vn.codegyme.meal_choice.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegyme.meal_choice.dto.AddressResponseDTO;
import vn.codegyme.meal_choice.dto.CreateAddressDTO;
import vn.codegyme.meal_choice.dto.UpdateAddressDTO;
import vn.codegyme.meal_choice.dto.UpdateProfileDTO;
import vn.codegyme.meal_choice.dto.UserResponseDTO;
import vn.codegyme.meal_choice.entity.Address;
import vn.codegyme.meal_choice.entity.User;
import vn.codegyme.meal_choice.repository.UserRepository;
import vn.codegyme.meal_choice.security.CustomUserDetails;
import vn.codegyme.meal_choice.service.UserService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserResponseDTO getCurrentUserProfile() {
        User user = getCurrentUser();
        return convertToDTO(user);
    }

    @Override
    @Transactional
    public UserResponseDTO updateProfile(UpdateProfileDTO updateProfileDTO) {
        User user = getCurrentUser();

        // Partial update: CHỈ cập nhật các trường được gửi lên (khác null)
        if (updateProfileDTO.getDisplayName() != null) {
            if (updateProfileDTO.getDisplayName().trim().isEmpty()) {
                throw new RuntimeException("Tên hiển thị không được để trống");
            }
            user.setDisplayName(updateProfileDTO.getDisplayName().trim());
        }
        if (updateProfileDTO.getDob() != null) {
            user.setDob(updateProfileDTO.getDob());
        }
        if (updateProfileDTO.getGender() != null) {
            user.setGender(updateProfileDTO.getGender());
        }
        if (updateProfileDTO.getAvatarUrl() != null) {
            user.setAvatarUrl(updateProfileDTO.getAvatarUrl());
        }

        User updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }

    @Override
    public List<AddressResponseDTO> getAllAddresses() {
        User user = getCurrentUser();
        return user.getAddresses().stream()
                .map(this::convertAddressToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AddressResponseDTO getAddressById(Long addressId) {
        User user = getCurrentUser();
        Address address = user.getAddresses().stream()
                .filter(a -> a.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ hoặc địa chỉ không thuộc về bạn"));
        return convertAddressToDTO(address);
    }

    @Override
    @Transactional
    public UserResponseDTO addAddress(CreateAddressDTO createAddressDTO) {
        User user = getCurrentUser();

        // Chặn trùng lặp: nếu request bị gửi nhiều lần (double-click, hoặc
        // request tự động gửi lại sau khi refresh token) với cùng nội dung địa
        // chỉ, không tạo thêm dòng mới mà dùng lại địa chỉ đã có.
        Address existing = findMatchingAddress(user, createAddressDTO);

        if (existing != null) {
            boolean wantsDefault = createAddressDTO.getIsDefault() != null && createAddressDTO.getIsDefault();

            if (wantsDefault && !Boolean.TRUE.equals(existing.getIsDefault())) {
                user.getAddresses().forEach(a -> a.setIsDefault(false));
                existing.setIsDefault(true);
            }

            User unchangedUser = userRepository.save(user);
            return convertToDTO(unchangedUser);
        }

        Address address = new Address();
        address.setContactName(createAddressDTO.getContactName());
        address.setContactPhone(createAddressDTO.getContactPhone());
        address.setCity(createAddressDTO.getCity());
        address.setDistrict(createAddressDTO.getDistrict());
        address.setWard(createAddressDTO.getWard());
        address.setStreet(createAddressDTO.getStreet());
        address.setNote(createAddressDTO.getNote());
        address.setIsDefault(createAddressDTO.getIsDefault() != null && createAddressDTO.getIsDefault());
        address.setUser(user);

        // Nếu đặt làm mặc định, bỏ mặc định của các địa chỉ khác
        if (address.getIsDefault()) {
            user.getAddresses().forEach(a -> a.setIsDefault(false));
        }

        user.getAddresses().add(address);
        User updatedUser = userRepository.save(user);

        return convertToDTO(updatedUser);
    }

    @Override
    @Transactional
    public UserResponseDTO updateAddress(Long addressId, UpdateAddressDTO updateAddressDTO) {
        User user = getCurrentUser();

        // Tìm địa chỉ thuộc sở hữu của user hiện tại
        Address address = user.getAddresses().stream()
                .filter(a -> a.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ hoặc địa chỉ không thuộc về bạn"));

        // Partial update: CHỈ cập nhật các trường được gửi lên (khác null)
        if (updateAddressDTO.getContactName() != null) {
            if (updateAddressDTO.getContactName().trim().isEmpty()) {
                throw new RuntimeException("Tên người nhận không được để trống");
            }
            address.setContactName(updateAddressDTO.getContactName().trim());
        }
        if (updateAddressDTO.getContactPhone() != null) {
            if (updateAddressDTO.getContactPhone().trim().isEmpty()) {
                throw new RuntimeException("Số điện thoại không được để trống");
            }
            address.setContactPhone(updateAddressDTO.getContactPhone().trim());
        }
        if (updateAddressDTO.getCity() != null) {
            if (updateAddressDTO.getCity().trim().isEmpty()) {
                throw new RuntimeException("Tỉnh/Thành phố không được để trống");
            }
            address.setCity(updateAddressDTO.getCity().trim());
        }
        if (updateAddressDTO.getDistrict() != null) {
            if (updateAddressDTO.getDistrict().trim().isEmpty()) {
                throw new RuntimeException("Quận/Huyện không được để trống");
            }
            address.setDistrict(updateAddressDTO.getDistrict().trim());
        }
        if (updateAddressDTO.getWard() != null) {
            if (updateAddressDTO.getWard().trim().isEmpty()) {
                throw new RuntimeException("Phường/Xã không được để trống");
            }
            address.setWard(updateAddressDTO.getWard().trim());
        }
        if (updateAddressDTO.getStreet() != null) {
            if (updateAddressDTO.getStreet().trim().isEmpty()) {
                throw new RuntimeException("Tên đường, Tòa nhà, Số nhà không được để trống");
            }
            address.setStreet(updateAddressDTO.getStreet().trim());
        }
        if (updateAddressDTO.getNote() != null) {
            address.setNote(updateAddressDTO.getNote().trim());
        }

        // Quản lý trạng thái mặc định
        if (updateAddressDTO.getIsDefault() != null) {
            if (updateAddressDTO.getIsDefault()) {
                user.getAddresses().forEach(a -> a.setIsDefault(a.getId().equals(addressId)));
            } else {
                address.setIsDefault(false);
            }
        }
// Nếu getIsDefault() == null -> giữ nguyên trạng thái isDefault cũ, không đụng vào

        User updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }

    @Override
    @Transactional
    public UserResponseDTO deleteAddress(Long addressId) {
        User user = getCurrentUser();

        Address address = user.getAddresses().stream()
                .filter(a -> a.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ hoặc địa chỉ không thuộc về bạn"));

        user.getAddresses().remove(address);
        User updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }

    @Override
    @Transactional
    public UserResponseDTO setDefaultAddress(Long addressId) {
        User user = getCurrentUser();

        boolean exists = user.getAddresses().stream()
                .anyMatch(a -> a.getId().equals(addressId));
        if (!exists) {
            throw new RuntimeException("Không tìm thấy địa chỉ hoặc địa chỉ không thuộc về bạn");
        }

        user.getAddresses().forEach(address ->
                address.setIsDefault(address.getId().equals(addressId))
        );

        User updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }

    // === Helper Methods ===

    /**
     * Tìm địa chỉ đã tồn tại của user có nội dung giống hệt request (so sánh
     * không phân biệt hoa/thường và khoảng trắng thừa). Dùng để chặn tạo
     * trùng lặp khi client gửi cùng một request lưu địa chỉ nhiều lần.
     */
    private Address findMatchingAddress(User user, CreateAddressDTO dto) {
        return user.getAddresses().stream()
                .filter(a -> sameText(a.getContactName(), dto.getContactName())
                        && sameText(a.getContactPhone(), dto.getContactPhone())
                        && sameText(a.getCity(), dto.getCity())
                        && sameText(a.getDistrict(), dto.getDistrict())
                        && sameText(a.getWard(), dto.getWard())
                        && sameText(a.getStreet(), dto.getStreet()))
                .findFirst()
                .orElse(null);
    }

    private boolean sameText(String a, String b) {
        String x = a == null ? "" : a.trim();
        String y = b == null ? "" : b.trim();
        return x.equalsIgnoreCase(y);
    }

    private User getCurrentUser() {
        org.springframework.security.core.Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new RuntimeException("Người dùng chưa đăng nhập");
        }

        UUID userId = userDetails.getId();

        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
    }

    private UserResponseDTO convertToDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getDisplayName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getAvatarUrl(),
                user.getDob() != null ? LocalDate.from(user.getDob().atStartOfDay()) : null,
                user.getGender(),
                user.getAddresses().stream()
                        .map(this::convertAddressToDTO)
                        .collect(Collectors.toList()),
                user.getIsActive());
    }

    private AddressResponseDTO convertAddressToDTO(Address address) {
        return new AddressResponseDTO(
                address.getId(),
                address.getContactName(),
                address.getContactPhone(),
                address.getCity(),
                address.getDistrict(),
                address.getWard(),
                address.getStreet(),
                address.getNote(),
                address.getIsDefault());
    }
}