package vn.codegyme.meal_choice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.codegyme.meal_choice.entity.ActivationToken;
import vn.codegyme.meal_choice.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface ActivationTokenRepository extends JpaRepository<ActivationToken, UUID> {

    Optional<ActivationToken> findByToken(String token);

    void deleteByUser(User user);
}
