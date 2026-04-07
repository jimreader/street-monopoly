package com.streetmonopoly.controller;

import com.streetmonopoly.dto.Dtos.*;
import com.streetmonopoly.service.GameService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/player")
public class PlayerController {

    @Autowired
    private GameService gameService;

    /**
     * Get the player's view of their game.
     * The X-Device-Token header binds this player to a single device.
     * First call sets the token; subsequent calls from a different device are rejected.
     */
    @GetMapping("/game/{joinToken}")
    public PlayerGameView getGameView(
            @PathVariable UUID joinToken,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken) {
        return gameService.getPlayerView(joinToken, deviceToken);
    }

    /**
     * Check in at a street location.
     */
    @PostMapping("/game/{joinToken}/checkin")
    public CheckInResponse checkIn(
            @PathVariable UUID joinToken,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken,
            @Valid @RequestBody CheckInRequest request) {
        return gameService.checkIn(joinToken, deviceToken, request);
    }
}
