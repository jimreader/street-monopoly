package com.streetmonopoly.controller;

import com.streetmonopoly.dto.Dtos.*;
import com.streetmonopoly.model.Game;
import com.streetmonopoly.model.GamePlayer;
import com.streetmonopoly.service.GameService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/games")
public class GameController {

    @Autowired
    private GameService gameService;

    @GetMapping
    public List<Game> getAllGames() {
        return gameService.getAllGames();
    }

    @GetMapping("/{id}")
    public Game getGame(@PathVariable UUID id) {
        return gameService.getGame(id);
    }

    @PostMapping
    public Game createGame(@Valid @RequestBody CreateGameRequest request) {
        return gameService.createGame(request);
    }

    @PostMapping("/{gameId}/invite")
    public GamePlayer invitePlayer(@PathVariable UUID gameId, @Valid @RequestBody InvitePlayerRequest request) {
        return gameService.invitePlayer(gameId, request);
    }

    @GetMapping("/{gameId}/players")
    public List<GamePlayer> getGamePlayers(@PathVariable UUID gameId) {
        return gameService.getGamePlayers(gameId);
    }

    @GetMapping("/{gameId}/admin-view")
    public AdminGameView getAdminView(@PathVariable UUID gameId) {
        return gameService.getAdminView(gameId);
    }
}
