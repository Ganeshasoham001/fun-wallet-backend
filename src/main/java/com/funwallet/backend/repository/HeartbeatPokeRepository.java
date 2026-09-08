package com.funwallet.backend.repository;

import com.funwallet.backend.model.HeartbeatPoke;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface HeartbeatPokeRepository extends JpaRepository<HeartbeatPoke, Long> {
    List<HeartbeatPoke> findByToUserAndAcknowledgedFalseOrderBySentAtDesc(String toUser);
    List<HeartbeatPoke> findByToUserAndSentAtAfterOrderBySentAtDesc(String toUser, Instant after);
    Optional<HeartbeatPoke> findTop1ByToUserOrderBySentAtDesc(String toUser);
}
