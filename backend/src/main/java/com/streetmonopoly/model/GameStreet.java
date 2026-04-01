package com.streetmonopoly.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class GameStreet {
    private UUID id;
    private UUID gameId;
    private UUID streetId;
    private UUID ownerPlayerId;
    private LocalDateTime purchasedAt;

    // Joined
    private Street street;
    private Player ownerPlayer;
}
