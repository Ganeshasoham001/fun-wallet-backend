package com.funwallet.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String role; // ADMIN (Soham), USER (Sairindhri)
    private int points = 100;
    
    private String email;
    private String password;

    private int studyStreak = 0;
    private java.time.LocalDate lastStudyDate;

    private int behaviourStreak = 0;
    private java.time.LocalDate lastBehaviourDate;
}
