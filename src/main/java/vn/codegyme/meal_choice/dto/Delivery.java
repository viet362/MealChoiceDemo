package vn.codegyme.meal_choice.dto;

public class Delivery {
    public record GeoPoint(
            double latitude,
            double longitude
    ) {
    }
}
