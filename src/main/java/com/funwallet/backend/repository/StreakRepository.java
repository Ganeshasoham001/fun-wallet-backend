package com.funwallet.backend.repository;

import com.funwallet.backend.model.Streak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StreakRepository extends JpaRepository<Streak, Long> {
    Optional<Streak> findByUserIdAndType(Long userId, String type);
}
