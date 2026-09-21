package vn.codegyme.meal_choice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.codegyme.meal_choice.entity.FoodCategory;

import java.util.Optional;

@Repository
public interface FoodCategoryRepository extends JpaRepository<FoodCategory, Long> {

    Optional<FoodCategory> findByCategoryName(String categoryName);

    Optional<FoodCategory> findByCategoryNameIgnoreCase(String categoryName);

    boolean existsByCategoryName(String categoryName);
}