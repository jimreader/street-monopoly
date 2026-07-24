package com.streetmonopoly.controller;

import com.streetmonopoly.dto.Dtos.*;
import com.streetmonopoly.model.Event;
import com.streetmonopoly.model.EventPlayer;
import com.streetmonopoly.model.Game;
import com.streetmonopoly.service.EventService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
public class EventController {

    @Autowired
    private EventService eventService;

    @GetMapping
    public List<Event> getAllEvents() {
        return eventService.getAllEvents();
    }

    @GetMapping("/{id}")
    public Event getEvent(@PathVariable UUID id) {
        return eventService.getEvent(id);
    }

    @PostMapping
    public Event createEvent(@Valid @RequestBody CreateEventRequest request) {
        return eventService.createEvent(request);
    }

    @PostMapping("/{eventId}/invite")
    public EventPlayer invitePlayer(@PathVariable UUID eventId, @Valid @RequestBody InvitePlayerRequest request) {
        return eventService.invitePlayer(eventId, request);
    }

    @GetMapping("/{eventId}/players")
    public List<EventPlayer> getEventPlayers(@PathVariable UUID eventId) {
        return eventService.getEventPlayers(eventId);
    }

    @GetMapping("/{eventId}/games")
    public List<Game> getEventGames(@PathVariable UUID eventId) {
        return eventService.getEventGames(eventId);
    }

    @GetMapping("/{eventId}/admin-view")
    public AdminEventView getAdminView(@PathVariable UUID eventId) {
        return eventService.getAdminEventView(eventId);
    }

    @PostMapping("/{eventId}/players/{eventPlayerId}/reset-device")
    public ResponseEntity<Void> resetPlayerDevice(
            @PathVariable UUID eventId,
            @PathVariable UUID eventPlayerId) {
        eventService.resetPlayerDevice(eventId, eventPlayerId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{eventId}/players/{eventPlayerId}")
    public ResponseEntity<Void> removePlayer(
            @PathVariable UUID eventId,
            @PathVariable UUID eventPlayerId) {
        eventService.removePlayer(eventId, eventPlayerId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID eventId) {
        eventService.deleteEvent(eventId);
        return ResponseEntity.noContent().build();
    }
}
