package com.funwallet.backend.repository;

import com.funwallet.backend.model.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
    List<PointHistory> findByUsernameOrderByTimestampDesc(String username);
}
