package com.streetmonopoly.controller;

import com.streetmonopoly.dto.Dtos.*;
import com.streetmonopoly.model.Event;
import com.streetmonopoly.model.EventChallenge;
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

    @GetMapping("/{eventId}/challenges")
    public List<ChallengeAdminView> getChallenges(@PathVariable UUID eventId) {
        return eventService.getEventChallenges(eventId);
    }

    @PostMapping("/{eventId}/challenges")
    public EventChallenge createChallenge(@PathVariable UUID eventId, @Valid @RequestBody CreateChallengeRequest request) {
        return eventService.createChallenge(eventId, request);
    }

    @PutMapping("/{eventId}/challenges/{challengeId}")
    public EventChallenge updateChallenge(
            @PathVariable UUID eventId,
            @PathVariable UUID challengeId,
            @Valid @RequestBody UpdateChallengeRequest request) {
        return eventService.updateChallenge(eventId, challengeId, request);
    }

    @DeleteMapping("/{eventId}/challenges/{challengeId}")
    public ResponseEntity<Void> deleteChallenge(
            @PathVariable UUID eventId,
            @PathVariable UUID challengeId) {
        eventService.deleteChallenge(eventId, challengeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{eventId}/challenges/{challengeId}/submissions")
    public List<ChallengeSubmissionAdminView> getChallengeSubmissions(
            @PathVariable UUID eventId,
            @PathVariable UUID challengeId) {
        return eventService.getChallengeSubmissions(eventId, challengeId);
    }

    @PostMapping("/{eventId}/challenges/{challengeId}/submissions/{submissionId}/review")
    public ResponseEntity<Void> reviewChallengeSubmission(
            @PathVariable UUID eventId,
            @PathVariable UUID challengeId,
            @PathVariable UUID submissionId,
            @Valid @RequestBody ReviewChallengeSubmissionRequest request) {
        eventService.reviewChallengeSubmission(eventId, challengeId, submissionId, request);
        return ResponseEntity.noContent().build();
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
