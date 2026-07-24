package com.streetmonopoly.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class EventChallenge {
    private UUID id;
    private UUID eventId;
    private String description;
    private BigDecimal prizeAmount;
    private int durationMinutes;
    private LocalDateTime scheduledStartAt;
    private LocalDateTime scheduledEndAt;
    private String status; // pending, active, completed
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
