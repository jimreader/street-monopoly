package com.streetmonopoly.controller;

import com.streetmonopoly.dto.Dtos.*;
import com.streetmonopoly.model.Game;
import com.streetmonopoly.model.GamePlayer;
import com.streetmonopoly.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/games")
public class GameController {

    @Autowired
    private GameService gameService;

    @GetMapping("/{id}")
    public Game getGame(@PathVariable UUID id) {
        return gameService.getGame(id);
    }

    @GetMapping("/{gameId}/players")
    public List<GamePlayer> getGamePlayers(@PathVariable UUID gameId) {
        return gameService.getGamePlayers(gameId);
    }

    @GetMapping("/{gameId}/admin-view")
    public AdminGameView getAdminView(@PathVariable UUID gameId) {
        return gameService.getAdminView(gameId);
    }

    @PostMapping("/{gameId}/players/{gamePlayerId}/reset-device")
    public ResponseEntity<Void> resetPlayerDevice(
            @PathVariable UUID gameId,
            @PathVariable UUID gamePlayerId) {
        gameService.resetPlayerDevice(gameId, gamePlayerId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{gameId}/players/{gamePlayerId}")
    public ResponseEntity<Void> removePlayer(
            @PathVariable UUID gameId,
            @PathVariable UUID gamePlayerId) {
        gameService.removePlayer(gameId, gamePlayerId);
        return ResponseEntity.noContent().build();
    }

}
