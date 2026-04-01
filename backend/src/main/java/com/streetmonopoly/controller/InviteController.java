package com.streetmonopoly.controller;

import com.streetmonopoly.model.GamePlayer;
import com.streetmonopoly.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/invite")
public class InviteController {

    @Autowired
    private GameService gameService;

    @Value("${app.player-url}")
    private String playerUrl;

    /**
     * When a player clicks their invite link, this accepts the invite and
     * redirects them to the player app with their join token.
     */
    @GetMapping("/{inviteToken}/accept")
    public ResponseEntity<Void> acceptInvite(@PathVariable UUID inviteToken) {
        GamePlayer gp = gameService.acceptInvite(inviteToken);
        String redirectUrl = playerUrl + "/game/" + gp.getJoinToken();
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }
}
