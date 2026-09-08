package com.funwallet.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Data
@Entity
@Table(name = "daily_questions")
public class DailyQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String questionDate; // e.g. "2026-09-09"

    @Column(length = 1000, nullable = false)
    private String questionText;

    @Column(length = 2000)
    private String sohamAnswer;

    @Column(length = 2000)
    private String sairindhriAnswer;

    private Instant sohamAnsweredAt;
    private Instant sairindhriAnsweredAt;

    private boolean revealed = false;

    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getQuestionDate() { return questionDate; }
    public void setQuestionDate(String questionDate) { this.questionDate = questionDate; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getSohamAnswer() { return sohamAnswer; }
    public void setSohamAnswer(String sohamAnswer) { this.sohamAnswer = sohamAnswer; }

    public String getSairindhriAnswer() { return sairindhriAnswer; }
    public void setSairindhriAnswer(String sairindhriAnswer) { this.sairindhriAnswer = sairindhriAnswer; }

    public Instant getSohamAnsweredAt() { return sohamAnsweredAt; }
    public void setSohamAnsweredAt(Instant sohamAnsweredAt) { this.sohamAnsweredAt = sohamAnsweredAt; }

    public Instant getSairindhriAnsweredAt() { return sairindhriAnsweredAt; }
    public void setSairindhriAnsweredAt(Instant sairindhriAnsweredAt) { this.sairindhriAnsweredAt = sairindhriAnsweredAt; }

    public boolean isRevealed() { return revealed; }
    public void setRevealed(boolean revealed) { this.revealed = revealed; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
