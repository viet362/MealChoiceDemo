package vn.codegyme.meal_choice.util;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.stream.Collectors;

public class AddressNormalizer {

    /**
     * Chuẩn hóa địa chỉ trước khi gửi sang API bản đồ.
     *
     * Các bước:
     * 1. Loại bỏ tiền tố hành chính.
     * 2. Chuyển tiếng Việt có dấu -> không dấu.
     * 3. Xóa khoảng trắng thừa và nối lại bằng dấu phẩy.
     */
    public static String normalize(String address) {

        if (address == null || address.trim().isEmpty()) {
            return "";
        }

        return Arrays.stream(address.split(","))

                // Xóa khoảng trắng đầu/cuối
                .map(String::trim)

                // Xóa tiền tố hành chính ở đầu từng thành phần
                .map(AddressNormalizer::removeAdministrativePrefix)

                // Chuyển sang không dấu
                .map(AddressNormalizer::removeVietnameseAccents)

                // Xóa khoảng trắng thừa
                .map(part -> part.replaceAll("\\s+", " ").trim())

                // Loại bỏ thành phần rỗng
                .filter(part -> !part.isEmpty())

                // Nối lại
                .collect(Collectors.joining(", "));
    }


    /**
     * Xóa:
     * Phường, Quận, Thành phố, Tỉnh,
     * Xã, Huyện, Thị trấn, Thị xã
     */
    private static String removeAdministrativePrefix(String text) {

        if (text == null) {
            return "";
        }

        return text.replaceFirst(
                "(?iu)^\\s*(Phường|Quận|Thành phố|Tỉnh|Xã|Huyện|Thị trấn|Thị xã)\\s+",
                ""
        );
    }


    /**
     * Chuyển tiếng Việt có dấu thành không dấu.
     */
    private static String removeVietnameseAccents(String text) {

        if (text == null) {
            return "";
        }

        String normalized = Normalizer.normalize(
                text,
                Normalizer.Form.NFD
        );

        // Loại bỏ dấu Unicode
        normalized = normalized.replaceAll(
                "\\p{M}+",
                ""
        );

        // Đ/đ không được Normalizer tự chuyển
        normalized = normalized
                .replace("Đ", "D")
                .replace("đ", "d");

        return normalized;
    }
}
