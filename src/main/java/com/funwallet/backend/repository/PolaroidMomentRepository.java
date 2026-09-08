package com.funwallet.backend.repository;

import com.funwallet.backend.model.PolaroidMoment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolaroidMomentRepository extends JpaRepository<PolaroidMoment, Long> {
    List<PolaroidMoment> findAllByOrderByMemoryDateDesc();
    List<PolaroidMoment> findAllByOrderByCreatedAtDesc();
}
