package vn.codegyme.meal_choice.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import vn.codegyme.meal_choice.dto.Delivery.GeoPoint;
import vn.codegyme.meal_choice.service.GeocodingService;
import vn.codegyme.meal_choice.util.AddressNormalizer;
import java.net.URI;

@Slf4j
@Service
@RequiredArgsConstructor
public class HeigitGeocodingService implements GeocodingService {

    private final RestTemplate restTemplate;

    @Value("${heigit.api-key:}")
    private String apiKey;

    // Tọa độ dự phòng mặc định (Hà Nội, Việt Nam)
    private static final GeoPoint DEFAULT_FALLBACK_POINT = new GeoPoint(21.028511, 105.854167);

    @Override
    public GeoPoint geocode(String address) {
        if (address == null || address.isBlank()) {
            return DEFAULT_FALLBACK_POINT;
        }

        try {
            String normalizedAddress = AddressNormalizer.normalize(address);
            log.debug("Geocoding address: {} -> Normalized: {}", address, normalizedAddress);

            // LẦN 1 - ĐỊA CHỈ ĐẦY ĐỦ
            GeoPoint result = searchAddress(normalizedAddress);
            if (result != null) {
                return result;
            }

            String[] parts = normalizedAddress.split(",");

            // LẦN 2 - BỎ PHƯỜNG
            if (parts.length >= 4) {
                String fallback1 = parts[0].trim() + ", " + parts[2].trim() + ", " + parts[3].trim();
                result = searchAddress(fallback1);
                if (result != null) {
                    return result;
                }
            }

            // LẦN 3 - CHỈ ĐƯỜNG + THÀNH PHỐ
            if (parts.length >= 4) {
                String fallback2 = parts[0].trim() + ", " + parts[3].trim();
                result = searchAddress(fallback2);
                if (result != null) {
                    return result;
                }
            }

            // LẦN 4 - THÀNH PHỐ
            if (parts.length >= 2) {
                String cityOnly = parts[parts.length - 1].trim();
                result = searchAddress(cityOnly);
                if (result != null) {
                    return result;
                }
            }
        } catch (Exception e) {
            log.warn("Lỗi khi geocoding địa chỉ '{}': {}", address, e.getMessage());
        }

        log.warn("Không thể tìm thấy tọa độ cụ thể cho '{}', sử dụng tọa độ dự phòng mặc định.", address);
        return DEFAULT_FALLBACK_POINT;
    }

    private GeoPoint searchAddress(String searchText) {
        try {
            // ==========================================
            // 1. TẠO URL VỚI FOCUS POINT THEO THÀNH PHỐ
            // ==========================================

            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(
                            "https://api.openrouteservice.org/geocode/search"
                    )
                    .queryParam(
                            "text",
                            searchText
                    )
                    .queryParam(
                            "size",
                            10
                    )
                    .queryParam(
                            "boundary.country",
                            "VNM"
                    );

            String lower = searchText.toLowerCase();
            if (lower.contains("ha noi") || lower.contains("hanoi")) {
                builder.queryParam("focus.point.lat", 21.028511)
                        .queryParam("focus.point.lon", 105.854167);
            } else if (lower.contains("ho chi minh") || lower.contains("hcm") || lower.contains("sai gon")) {
                builder.queryParam("focus.point.lat", 10.776889)
                        .queryParam("focus.point.lon", 106.700806);
            } else if (lower.contains("da nang")) {
                builder.queryParam("focus.point.lat", 16.054407)
                        .queryParam("focus.point.lon", 108.202167);
            }

            URI uri = builder.build().encode().toUri();

            log.debug("GEOCODING SEARCH: {} -> REQUEST URL: {}", searchText, uri);

            // ==========================================
            // 2. HEADER
            // ==========================================

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", apiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // ==========================================
            // 3. GỌI HEIGIT / PELIAS API
            // ==========================================

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    entity,
                    JsonNode.class
            );

            JsonNode root = response.getBody();
            if (root == null) {
                return null;
            }

            // ==========================================
            // 4. LẤY DANH SÁCH KẾT QUẢ
            // ==========================================

            JsonNode features = root.path("features");
            if (!features.isArray() || features.isEmpty()) {
                return null;
            }

            // ==========================================
            // 5. DUYỆT TỪNG KẾT QUẢ VÀ LỰA CHỌN PHÙ HỢP
            // ==========================================

            for (JsonNode feature : features) {
                JsonNode properties = feature.path("properties");
                String layer = properties.path("layer").asText("");
                String label = properties.path("label").asText("");

                // Chấp nhận các layer từ chi tiết đến cấp quận/huyện, phường/xã
                boolean acceptableLayer = "address".equalsIgnoreCase(layer)
                        || "street".equalsIgnoreCase(layer)
                        || "venue".equalsIgnoreCase(layer)
                        || "neighbourhood".equalsIgnoreCase(layer)
                        || "locality".equalsIgnoreCase(layer)
                        || "localadmin".equalsIgnoreCase(layer)
                        || "borough".equalsIgnoreCase(layer)
                        || "county".equalsIgnoreCase(layer)
                        || "region".equalsIgnoreCase(layer);

                if (!acceptableLayer) {
                    continue;
                }

                JsonNode coordinates = feature.path("geometry").path("coordinates");
                if (!coordinates.isArray() || coordinates.size() < 2) {
                    continue;
                }

                double longitude = coordinates.get(0).asDouble();
                double latitude = coordinates.get(1).asDouble();

                if (latitude == 0 || longitude == 0) {
                    continue;
                }

                // Kiểm tra tính hợp lệ về mặt địa lý với thành phố yêu cầu
                if (lower.contains("ha noi") || lower.contains("hanoi")) {
                    if (latitude < 20.0 || latitude > 22.0 || longitude < 104.5 || longitude > 107.0) {
                        // Tọa độ nằm ngoài khu vực Hà Nội -> Bỏ qua kết quả nhầm tỉnh khác
                        continue;
                    }
                } else if (lower.contains("ho chi minh") || lower.contains("hcm") || lower.contains("sai gon")) {
                    if (latitude < 10.0 || latitude > 11.5 || longitude < 106.0 || longitude > 107.5) {
                        continue;
                    }
                } else if (lower.contains("da nang")) {
                    if (latitude < 15.5 || latitude > 16.5 || longitude < 107.5 || longitude > 108.8) {
                        continue;
                    }
                }

                log.info("Geocoding matched: '{}' -> Lat: {}, Lon: {} (Layer: {})", label, latitude, longitude, layer);
                return new GeoPoint(latitude, longitude);
            }

            return null;

        } catch (Exception e) {
            log.warn("Lỗi khi tìm kiếm tọa độ cho '{}': {}", searchText, e.getMessage());
            return null;
        }
    }
}
