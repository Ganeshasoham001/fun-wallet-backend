package com.funwallet.backend.repository;

import com.funwallet.backend.model.LoveEnvelope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoveEnvelopeRepository extends JpaRepository<LoveEnvelope, Long> {
    List<LoveEnvelope> findAllByOrderByCreatedAtDesc();
    List<LoveEnvelope> findByRecipientOrRecipientIsNullOrderByCreatedAtDesc(String recipient);
}
