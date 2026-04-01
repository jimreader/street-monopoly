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
     * Get the player's view of their game (uses join token for auth)
     */
    @GetMapping("/game/{joinToken}")
    public PlayerGameView getGameView(@PathVariable UUID joinToken) {
        return gameService.getPlayerView(joinToken);
    }

    /**
     * Check in at a street location
     */
    @PostMapping("/game/{joinToken}/checkin")
    public CheckInResponse checkIn(@PathVariable UUID joinToken, @Valid @RequestBody CheckInRequest request) {
        return gameService.checkIn(joinToken, request);
    }
}
