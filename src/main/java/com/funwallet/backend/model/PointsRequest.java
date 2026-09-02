package com.funwallet.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class PointsRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String category; // GOOD_BEHAVIOR, STUDY
    private int requestedPoints;
    private String status; // PENDING, APPROVED, REJECTED
}
