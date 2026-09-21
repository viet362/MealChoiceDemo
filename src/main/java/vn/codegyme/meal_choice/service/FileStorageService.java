package vn.codegyme.meal_choice.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadRoot = Paths.get("uploads", "foods");
    private final Path settlementUploadRoot = Paths.get("uploads", "settlements");
    private final Path payoutUploadRoot = Paths.get("uploads", "payouts");

    // Lưu ảnh món ăn
    public String saveFoodImage(Long foodId, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new RuntimeException("Ảnh món ăn không được để trống");
        }

        String originalFileName = image.getOriginalFilename();

        if (originalFileName == null || originalFileName.isBlank()) {
            throw new RuntimeException("Tên file ảnh không hợp lệ");
        }

        String extension = getFileExtension(originalFileName);

        if (!isImageExtension(extension)) {
            throw new RuntimeException("File tải lên phải là ảnh");
        }

        String fileName = UUID.randomUUID() + extension;
        Path foodDirectory = uploadRoot.resolve(foodId.toString());
        Path targetFile = foodDirectory.resolve(fileName);

        try {
            Files.createDirectories(foodDirectory);

            Files.copy(
                    image.getInputStream(),
                    targetFile,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return "/uploads/foods/" + foodId + "/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Không thể lưu ảnh món ăn", e);
        }
    }

    // Lưu ảnh bằng chứng khiếu nại đối soát
    public String saveSettlementEvidenceImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return null;
        }

        String originalFileName = image.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new RuntimeException("Tên file ảnh không hợp lệ");
        }

        String extension = getFileExtension(originalFileName);
        if (!isImageExtension(extension)) {
            throw new RuntimeException("File tải lên phải là ảnh (jpg, jpeg, jfif, png, webp, gif, bmp)");
        }

        String fileName = UUID.randomUUID() + extension;
        Path targetFile = settlementUploadRoot.resolve(fileName);

        try {
            Files.createDirectories(settlementUploadRoot);

            Files.copy(
                    image.getInputStream(),
                    targetFile,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return "/uploads/settlements/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Không thể lưu ảnh bằng chứng khiếu nại", e);
        }
    }

    // Lưu ảnh ủy nhiệm chi / chứng từ chuyển khoản Payout
    public String savePayoutProofImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new RuntimeException("Vui lòng tải lên ảnh chứng từ chuyển khoản");
        }

        String originalFileName = image.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new RuntimeException("Tên file ảnh không hợp lệ");
        }

        String extension = getFileExtension(originalFileName);
        if (!isImageExtension(extension)) {
            throw new RuntimeException("File tải lên phải là ảnh (jpg, jpeg, jfif, png, webp, gif, bmp)");
        }

        String fileName = UUID.randomUUID() + extension;
        Path targetFile = payoutUploadRoot.resolve(fileName);

        try {
            Files.createDirectories(payoutUploadRoot);

            Files.copy(
                    image.getInputStream(),
                    targetFile,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return "/uploads/payouts/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Không thể lưu ảnh chứng từ chuyển khoản", e);
        }
    }

    // Xóa ảnh
    public void deleteFoodImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        try {
            String relativePath = imageUrl.startsWith("/")
                    ? imageUrl.substring(1)
                    : imageUrl;

            Path filePath = Paths.get(relativePath);

            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Không thể xóa ảnh món ăn", e);
        }
    }

    // Lấy phần mở rộng của file
    private String getFileExtension(String fileName) {
        int index = fileName.lastIndexOf(".");

        if (index == -1) {
            return "";
        }

        return fileName.substring(index).toLowerCase();
    }

    // Kiểm tra định dạng ảnh
    private boolean isImageExtension(String extension) {
        String ext = extension.toLowerCase();
        return ext.equals(".jpg")
                || ext.equals(".jpeg")
                || ext.equals(".jfif")
                || ext.equals(".png")
                || ext.equals(".webp")
                || ext.equals(".gif")
                || ext.equals(".bmp")
                || ext.equals(".svg");
    }
}