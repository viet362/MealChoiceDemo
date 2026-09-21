package vn.codegyme.meal_choice.service;


import vn.codegyme.meal_choice.dto.Delivery.GeoPoint;

public interface DistanceService {

    double calculateDistanceKm(
            GeoPoint from,
            GeoPoint to
    );
}
