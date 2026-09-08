package com.funwallet.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "love_envelopes")
public class LoveEnvelope {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title; // e.g. "Open when you are having a tough day 🥺"

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content; // Secret letter/note

    private String sender; // Soham or Sairindhri
    private String recipient; // Partner name

    private String unlockType = "ANYTIME"; // ANYTIME, DATE_LOCKED
    private LocalDate unlockDate; // For anniversary or specific future date

    private boolean isOpened = false;
    private Instant openedAt;

    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public String getUnlockType() { return unlockType; }
    public void setUnlockType(String unlockType) { this.unlockType = unlockType; }

    public LocalDate getUnlockDate() { return unlockDate; }
    public void setUnlockDate(LocalDate unlockDate) { this.unlockDate = unlockDate; }

    public boolean isOpened() { return isOpened; }
    public void setOpened(boolean opened) { isOpened = opened; }

    public Instant getOpenedAt() { return openedAt; }
    public void setOpenedAt(Instant openedAt) { this.openedAt = openedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
