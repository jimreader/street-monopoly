package com.streetmonopoly.service;

import java.util.UUID;

public record PlayerInvitedEvent(UUID eventPlayerId, String email, String playerName, String eventName, UUID joinToken) {
}
