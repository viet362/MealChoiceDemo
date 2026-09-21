package vn.codegyme.meal_choice.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FoodEntityTest {

    @Test
    void builderShouldInitializeImageCollection() {
        Food food = Food.builder().build();

        assertNotNull(food.getImages());
        assertTrue(food.getImages().isEmpty());
    }
}
