package com.streetmonopoly.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class EventPlayer {
    private UUID id;
    private UUID eventId;
    private UUID playerId;
    private UUID assignedGameId;
    private UUID inviteToken;
    private UUID joinToken;
    private LocalDateTime invitedAt;
    private LocalDateTime joinedAt;
    private String deviceToken;
    private LocalDateTime deletedAt;

    // Joined
    private Player player;
}
