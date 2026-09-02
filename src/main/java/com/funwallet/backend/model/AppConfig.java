package com.funwallet.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class AppConfig {
    @Id
    private Long id = 1L; // Single row

    private int angerDeduction = 6;
    private int studyGain = 5;
    private int studyRequiredDays = 4;
    private int behaviourGain = 10;
    private int behaviourRequiredDays = 10;
}
