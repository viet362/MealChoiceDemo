package vn.codegyme.meal_choice.service;

import vn.codegyme.meal_choice.dto.Delivery.GeoPoint;

public interface GeocodingService {

    GeoPoint geocode(String address);
}
