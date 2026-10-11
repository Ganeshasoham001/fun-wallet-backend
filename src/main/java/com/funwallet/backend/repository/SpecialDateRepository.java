package com.funwallet.backend.repository;

import com.funwallet.backend.model.SpecialDate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpecialDateRepository extends JpaRepository<SpecialDate, Long> {
    List<SpecialDate> findAllByOrderByEventDateAsc();
}
