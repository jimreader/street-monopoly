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

    @GetMapping("/game/{joinToken}/summary")
    public PlayerJoinSummary getJoinSummary(@PathVariable UUID joinToken) {
        return gameService.getPlayerJoinSummary(joinToken);
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

    @GetMapping("/game/{joinToken}/challenges")
    public java.util.List<PlayerChallengeView> getChallenges(
            @PathVariable UUID joinToken,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken) {
        return gameService.getPlayerChallenges(joinToken, deviceToken);
    }

    @PostMapping("/game/{joinToken}/challenges/{challengeId}/submit")
    public void submitChallenge(
            @PathVariable UUID joinToken,
            @PathVariable UUID challengeId,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken,
            @Valid @RequestBody ChallengeSubmissionRequest request) {
        gameService.submitChallenge(joinToken, deviceToken, challengeId, request);
    }
}
