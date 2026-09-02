package com.funwallet.backend.repository;

import com.funwallet.backend.model.PointsRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointsRequestRepository extends JpaRepository<PointsRequest, Long> {
    List<PointsRequest> findByStatus(String status);
}
