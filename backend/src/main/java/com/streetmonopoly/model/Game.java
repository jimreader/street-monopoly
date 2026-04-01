package com.streetmonopoly.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class Game {
    private UUID id;
    private String name;
    private UUID gameMapId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal startingBalance;
    private int proximityMetres;
    private String status; // pending, active, completed
    private LocalDateTime createdAt;

    // Joined fields
    private GameMap gameMap;
}
