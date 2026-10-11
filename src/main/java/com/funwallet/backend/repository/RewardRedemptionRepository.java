package com.funwallet.backend.repository;

import com.funwallet.backend.model.RewardRedemption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RewardRedemptionRepository extends JpaRepository<RewardRedemption, Long> {
    List<RewardRedemption> findByStatusOrderByRedeemedAtDesc(String status);
    List<RewardRedemption> findAllByOrderByRedeemedAtDesc();
    List<RewardRedemption> findByRedeemedByOrderByRedeemedAtDesc(String redeemedBy);
    List<RewardRedemption> findByFulfilledByAndStatusOrderByRedeemedAtDesc(String fulfilledBy, String status);
}
