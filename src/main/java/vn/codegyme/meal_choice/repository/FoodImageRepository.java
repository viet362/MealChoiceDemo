package vn.codegyme.meal_choice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.codegyme.meal_choice.entity.FoodImage;

@Repository
public interface FoodImageRepository extends JpaRepository<FoodImage, Long> {
}
