package com.streetmonopoly.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class GamePlayer {
    private UUID id;
    private UUID gameId;
    private UUID playerId;
    private BigDecimal balance;
    private UUID inviteToken;
    private UUID joinToken;
    private LocalDateTime invitedAt;
    private LocalDateTime joinedAt;
    private BigDecimal finalBalance;
    private String deviceToken;

    // Joined
    private Player player;
    private Game game;
}
