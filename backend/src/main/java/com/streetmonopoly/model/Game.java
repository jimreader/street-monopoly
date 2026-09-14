package com.streetmonopoly.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class Game {
    private UUID id;
    private UUID eventId;
    private String name;
    private UUID gameMapId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal startingBalance;
    private int proximityMetres;
    private int unvisitedStreetPenaltyPercent;
    private String status; // pending, active, completed
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    // Joined fields
    private GameMap gameMap;
}
