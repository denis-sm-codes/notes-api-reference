package repository;

import entity.User; // Исправили здесь
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findById(Long id);

    void deleteByUsername(String name);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    List<User> findByCreatedAtBetween(ZonedDateTime start, ZonedDateTime end);
}