package com.streetmonopoly.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class Event {
    private UUID id;
    private String name;
    private String logoImageUrl;
    private UUID gameMapId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal startingBalance;
    private int proximityMetres;
    private int maxPlayersPerGame;
    private int unvisitedStreetPenaltyPercent;
    private String status; // pending, active, completed
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
}
