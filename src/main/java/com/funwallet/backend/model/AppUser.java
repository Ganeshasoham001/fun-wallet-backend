package com.funwallet.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private LocalDate lastStudyDate;

    private int behaviourStreak = 0;
    private LocalDate lastBehaviourDate;
    
    @Column(length = 500)
    private String moodText;
    
    private LocalDateTime moodUpdatedAt;

    public Long getId() {
        return id;
    }
    
    public String getMoodText() {
        return moodText;
    }
    
    public void setMoodText(String moodText) {
        this.moodText = moodText;
    }
    
    public LocalDateTime getMoodUpdatedAt() {
        return moodUpdatedAt;
    }
    
    public void setMoodUpdatedAt(LocalDateTime moodUpdatedAt) {
        this.moodUpdatedAt = moodUpdatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
