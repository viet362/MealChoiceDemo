package vn.codegyme.meal_choice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.codegyme.meal_choice.entity.Tag;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
}