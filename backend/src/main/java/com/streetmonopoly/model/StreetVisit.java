package com.streetmonopoly.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class StreetVisit {
    private UUID id;
    private UUID gameId;
    private UUID streetId;
    private UUID playerId;
    private String visitType; // purchased, rent_paid, insufficient_funds
    private BigDecimal amount;
    private LocalDateTime visitedAt;
}
