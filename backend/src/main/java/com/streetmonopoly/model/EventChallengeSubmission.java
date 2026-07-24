package com.streetmonopoly.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class EventChallengeSubmission {
    private UUID id;
    private UUID challengeId;
    private UUID eventPlayerId;
    private String photoUrl;
    private LocalDateTime submittedAt;
    private String reviewStatus; // pending, accomplished, failed
    private String reviewNotes;
    private LocalDateTime reviewedAt;
    private BigDecimal prizeAwardedAmount;
}
