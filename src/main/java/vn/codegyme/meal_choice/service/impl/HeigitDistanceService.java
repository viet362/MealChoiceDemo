package vn.codegyme.meal_choice.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import vn.codegyme.meal_choice.dto.Delivery.GeoPoint;
import vn.codegyme.meal_choice.service.DistanceService;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HeigitDistanceService implements DistanceService {

    private final RestTemplate restTemplate;

    @Value("${heigit.api-key:}")
    private String apiKey;

    @Value("${heigit.directions-url:https://api.openrouteservice.org/v2/directions/driving-car}")
    private String directionsUrl;

    @Override
    public double calculateDistanceKm(GeoPoint from, GeoPoint to) {
        if (from == null || to == null) {
            return 3.0;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", apiKey);

            Map<String, Object> body = Map.of(
                    "coordinates",
                    List.of(
                            List.of(from.longitude(), from.latitude()),
                            List.of(to.longitude(), to.latitude())
                    )
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    directionsUrl,
                    HttpMethod.POST,
                    entity,
                    JsonNode.class
            );

            JsonNode root = response.getBody();
            if (root != null && !root.path("routes").isEmpty()) {
                double distanceMeters = root.path("routes")
                        .get(0)
                        .path("summary")
                        .path("distance")
                        .asDouble();
                return Math.round((distanceMeters / 1000.0) * 10.0) / 10.0;
            }
        } catch (Exception e) {
            log.warn("Không thể tính khoảng cách qua OpenRouteService: {}. Dùng khoảng cách ước lượng Haversine.", e.getMessage());
        }

        // Fallback: Ước lượng khoảng cách theo công thức Haversine (nhân hệ số đường bộ 1.3)
        return calculateHaversineKm(from, to);
    }

    private double calculateHaversineKm(GeoPoint from, GeoPoint to) {
        try {
            double lat1 = Math.toRadians(from.latitude());
            double lon1 = Math.toRadians(from.longitude());
            double lat2 = Math.toRadians(to.latitude());
            double lon2 = Math.toRadians(to.longitude());

            double dlat = lat2 - lat1;
            double dlon = lon2 - lon1;

            double a = Math.sin(dlat / 2) * Math.sin(dlat / 2)
                    + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dlon / 2) * Math.sin(dlon / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            double distance = 6371.0 * c; // Earth radius in km

            double estimatedRoadDistance = Math.max(1.0, distance * 1.3);
            return Math.round(estimatedRoadDistance * 10.0) / 10.0;
        } catch (Exception ex) {
            return 3.0;
        }
    }
}
