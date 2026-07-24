package com.streetmonopoly.service;

import com.streetmonopoly.dto.Dtos.*;
import com.streetmonopoly.mapper.*;
import com.streetmonopoly.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class EventService {

    @Autowired private EventMapper eventMapper;
    @Autowired private EventPlayerMapper eventPlayerMapper;
    @Autowired private GameMapper gameMapper;
    @Autowired private GameMapMapper gameMapMapper;
    @Autowired private GamePlayerMapper gamePlayerMapper;
    @Autowired private GameStreetMapper gameStreetMapper;
    @Autowired private StreetVisitMapper streetVisitMapper;
    @Autowired private PlayerMapper playerMapper;
    @Autowired private EmailService emailService;
    @Autowired private GameService gameService;

    public List<Event> getAllEvents() {
        return eventMapper.findAll();
    }

    public Event getEvent(UUID eventId) {
        Event event = eventMapper.findById(eventId);
        if (event == null) throw new RuntimeException("Event not found: " + eventId);
        return event;
    }

    @Transactional
    public Event createEvent(CreateEventRequest request) {
        GameMap map = gameMapMapper.findById(request.getGameMapId());
        if (map == null) throw new RuntimeException("Game map not found");

        if (request.getEndTime().isBefore(request.getStartTime()) || request.getEndTime().isEqual(request.getStartTime())) {
            throw new RuntimeException("End time must be after start time");
        }

        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setName(request.getName());
        event.setGameMapId(request.getGameMapId());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setStartingBalance(request.getStartingBalance());
        event.setProximityMetres(request.getProximityMetres());
        event.setMaxPlayersPerGame(request.getMaxPlayersPerGame());
        event.setStatus("pending");
        eventMapper.insert(event);
        return eventMapper.findById(event.getId());
    }

    @Transactional
    public EventPlayer invitePlayer(UUID eventId, InvitePlayerRequest request) {
        Event event = getEvent(eventId);
        String normalizedEmail = request.getEmail() == null ? null : request.getEmail().trim();

        if (!"pending".equals(event.getStatus()) && !"active".equals(event.getStatus())) {
            throw new RuntimeException("Players can only be invited to pending or active events");
        }

        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new RuntimeException("Email is required");
        }

        if (eventPlayerMapper.findActiveByEventAndEmail(eventId, normalizedEmail) != null) {
            throw new RuntimeException("A player with this email is already invited to this event");
        }

        Player player = playerMapper.findByEmail(normalizedEmail);
        if (player == null) {
            player = new Player();
            player.setId(UUID.randomUUID());
            player.setName(request.getName());
            player.setEmail(normalizedEmail);
            playerMapper.insert(player);
        } else {
            player.setName(request.getName());
            playerMapper.update(player);
        }

        EventPlayer existing = eventPlayerMapper.findByEventAndPlayer(eventId, player.getId());
        if (existing != null) {
            throw new RuntimeException("Player already invited to this event");
        }

        EventPlayer deletedLink = eventPlayerMapper.findByEventAndPlayerIncludingDeleted(eventId, player.getId());
        if (deletedLink != null) {
            UUID inviteToken = UUID.randomUUID();
            UUID joinToken = UUID.randomUUID();
            eventPlayerMapper.restorePlayer(deletedLink.getId(), inviteToken, joinToken);
            emailService.sendJoinEmail(player.getEmail(), player.getName(), event.getName(), joinToken);

            EventPlayer restored = eventPlayerMapper.findByEventAndId(eventId, deletedLink.getId());
            restored.setPlayer(player);
            return restored;
        }

        EventPlayer ep = new EventPlayer();
        ep.setId(UUID.randomUUID());
        ep.setEventId(eventId);
        ep.setPlayerId(player.getId());
        ep.setInviteToken(UUID.randomUUID());
        ep.setJoinToken(UUID.randomUUID());
        eventPlayerMapper.insert(ep);
        eventPlayerMapper.markJoined(ep.getId());

        emailService.sendJoinEmail(player.getEmail(), player.getName(), event.getName(), ep.getJoinToken());

        ep.setPlayer(player);
        return ep;
    }

    public List<EventPlayer> getEventPlayers(UUID eventId) {
        getEvent(eventId);
        return eventPlayerMapper.findByEventId(eventId);
    }

    public List<Game> getEventGames(UUID eventId) {
        getEvent(eventId);
        return gameMapper.findByEventId(eventId);
    }

    public AdminEventView getAdminEventView(UUID eventId) {
        Event event = getEvent(eventId);
        GameMap map = gameMapMapper.findById(event.getGameMapId());
        List<Game> games = gameMapper.findByEventId(eventId);
        List<EventPlayer> eventPlayers = eventPlayerMapper.findByEventId(eventId);

        AdminEventView view = new AdminEventView();
        view.setEventId(event.getId());
        view.setEventName(event.getName());
        view.setStatus(event.getStatus());
        view.setStartTime(event.getStartTime());
        view.setEndTime(event.getEndTime());
        view.setStartingBalance(event.getStartingBalance());
        view.setProximityMetres(event.getProximityMetres());
        view.setMaxPlayersPerGame(event.getMaxPlayersPerGame());
        view.setMapName(map != null ? map.getName() : null);

        List<EventGameSummary> gameSummaries = new ArrayList<>();
        for (Game game : games) {
            EventGameSummary gs = new EventGameSummary();
            gs.setGameId(game.getId());
            gs.setGameName(game.getName());
            gs.setStatus(game.getStatus());
            gs.setPlayerCount(gamePlayerMapper.findAllByGameId(game.getId()).size());
            gameSummaries.add(gs);
        }
        view.setGames(gameSummaries);
        view.setLeaderboard(buildEventLeaderboard(event, games, eventPlayers));

        return view;
    }

    private List<LeaderboardEntry> buildEventLeaderboard(Event event, List<Game> games, List<EventPlayer> eventPlayers) {
        List<LeaderboardEntry> leaderboard = new ArrayList<>();

        for (EventPlayer eventPlayer : eventPlayers) {
            Player player = playerMapper.findById(eventPlayer.getPlayerId());
            if (player == null) continue;

            GamePlayer gamePlayer = gamePlayerMapper.findByEventPlayerId(eventPlayer.getId());

            LeaderboardEntry entry = new LeaderboardEntry();
            entry.setPlayerId(player.getId());
            entry.setPlayerName(player.getName());

            if (gamePlayer != null) {
                entry.setBalance(gamePlayer.getBalance());
                entry.setFinalBalance(gamePlayer.getFinalBalance());

                int streetsOwned = 0;
                List<RentCollection> rentCollections = new ArrayList<>();
                for (Game game : games) {
                    streetsOwned += gameStreetMapper.countOwnedByPlayer(game.getId(), player.getId());
                    rentCollections.addAll(buildRentCollectionsForOwner(game.getId(), player.getId()));
                }
                entry.setStreetsOwned(streetsOwned);
                entry.setRentCollections(rentCollections);
            } else {
                entry.setBalance(event.getStartingBalance());
                entry.setFinalBalance(null);
                entry.setStreetsOwned(0);
                entry.setRentCollections(Collections.emptyList());
            }

            leaderboard.add(entry);
        }

        leaderboard.sort((a, b) -> {
            BigDecimal scoreA = resolveLeaderboardScore(event.getStatus(), a);
            BigDecimal scoreB = resolveLeaderboardScore(event.getStatus(), b);
            return scoreB.compareTo(scoreA);
        });

        for (int i = 0; i < leaderboard.size(); i++) {
            leaderboard.get(i).setRank(i + 1);
        }

        return leaderboard;
    }

    private BigDecimal resolveLeaderboardScore(String eventStatus, LeaderboardEntry entry) {
        if ("completed".equals(eventStatus) && entry.getFinalBalance() != null) {
            return entry.getFinalBalance();
        }
        return entry.getBalance() == null ? BigDecimal.ZERO : entry.getBalance();
    }

    private List<RentCollection> buildRentCollectionsForOwner(UUID gameId, UUID ownerPlayerId) {
        List<RentCollection> collections = new ArrayList<>();
        List<StreetVisit> rentVisits = streetVisitMapper.findRentPaidToOwner(gameId, ownerPlayerId);
        for (StreetVisit rv : rentVisits) {
            Street street = gameMapMapper.findStreetById(rv.getStreetId());
            Player payer = playerMapper.findById(rv.getPlayerId());
            if (street != null && payer != null) {
                RentCollection rc = new RentCollection();
                rc.setStreetName(street.getName());
                rc.setStreetColour(street.getColour());
                rc.setPaidByPlayerName(payer.getName());
                rc.setAmount(rv.getAmount());
                rc.setCollectedAt(rv.getVisitedAt());
                collections.add(rc);
            }
        }
        return collections;
    }

    public void resetPlayerDevice(UUID eventId, UUID eventPlayerId) {
        EventPlayer ep = eventPlayerMapper.findByEventAndId(eventId, eventPlayerId);
        if (ep == null) throw new RuntimeException("Player not found in this event");

        eventPlayerMapper.clearDeviceToken(eventPlayerId);
        GamePlayer gp = gamePlayerMapper.findByEventPlayerId(eventPlayerId);
        if (gp != null) {
            gamePlayerMapper.clearDeviceToken(gp.getId());
        }
    }

    @Transactional
    public void removePlayer(UUID eventId, UUID eventPlayerId) {
        Event event = getEvent(eventId);
        if ("completed".equals(event.getStatus())) {
            throw new RuntimeException("Cannot remove players from a completed event");
        }

        EventPlayer ep = eventPlayerMapper.findByEventAndId(eventId, eventPlayerId);
        if (ep == null) throw new RuntimeException("Player not found in this event");

        if (ep.getAssignedGameId() != null) {
            GamePlayer gp = gamePlayerMapper.findByEventPlayerId(ep.getId());
            if (gp != null) {
                gameStreetMapper.clearOwnerByGameAndPlayer(gp.getGameId(), gp.getPlayerId());
                gamePlayerMapper.softDelete(gp.getGameId(), gp.getId());
            }
        }

        int updated = eventPlayerMapper.softDelete(eventId, eventPlayerId);
        if (updated == 0) {
            throw new RuntimeException("Player not found in this event");
        }
    }

    @Transactional
    public void deleteEvent(UUID eventId) {
        Event event = getEvent(eventId);
        if ("active".equals(event.getStatus())) {
            throw new RuntimeException("Cannot delete an event while it is in progress");
        }

        List<Game> games = gameMapper.findByEventId(eventId);
        for (Game game : games) {
            gamePlayerMapper.softDeleteByGameId(game.getId());
            gameMapper.softDelete(game.getId());
        }

        eventPlayerMapper.softDeleteByEventId(eventId);
        int updated = eventMapper.softDelete(eventId);
        if (updated == 0) {
            throw new RuntimeException("Event not found: " + eventId);
        }
    }

    @Scheduled(fixedRate = 15000)
    @Transactional
    public void updateEventStatuses() {
        List<Event> toStart = eventMapper.findPendingReadyToStart();
        for (Event event : toStart) {
            startEvent(event);
        }

        List<Event> toComplete = eventMapper.findActiveReadyToEnd();
        for (Event event : toComplete) {
            completeEvent(event);
        }
    }

    @Transactional
    public void startEvent(Event event) {
        if (!"pending".equals(event.getStatus())) return;

        List<EventPlayer> players = eventPlayerMapper.findByEventId(event.getId());
        if (players.isEmpty()) {
            eventMapper.updateStatus(event.getId(), "active");
            return;
        }

        int maxPlayersPerGame = Math.max(1, event.getMaxPlayersPerGame());
        int gameCount = (int) Math.ceil((double) players.size() / maxPlayersPerGame);

        List<Street> streets = gameMapMapper.findStreetsByMapId(event.getGameMapId());
        List<Game> generatedGames = new ArrayList<>();

        for (int i = 0; i < gameCount; i++) {
            Game game = new Game();
            game.setId(UUID.randomUUID());
            game.setEventId(event.getId());
            game.setName(event.getName() + " - Game " + (i + 1));
            game.setGameMapId(event.getGameMapId());
            game.setStartTime(event.getStartTime());
            game.setEndTime(event.getEndTime());
            game.setStartingBalance(event.getStartingBalance());
            game.setProximityMetres(event.getProximityMetres());
            game.setStatus("active");
            gameMapper.insert(game);

            for (Street street : streets) {
                GameStreet gs = new GameStreet();
                gs.setId(UUID.randomUUID());
                gs.setGameId(game.getId());
                gs.setStreetId(street.getId());
                gameStreetMapper.insert(gs);
            }

            generatedGames.add(game);
        }

        for (int i = 0; i < players.size(); i++) {
            EventPlayer ep = players.get(i);
            Game targetGame = generatedGames.get(i % generatedGames.size());

            eventPlayerMapper.assignToGame(ep.getId(), targetGame.getId());

            GamePlayer gp = new GamePlayer();
            gp.setId(UUID.randomUUID());
            gp.setGameId(targetGame.getId());
            gp.setEventPlayerId(ep.getId());
            gp.setPlayerId(ep.getPlayerId());
            gp.setBalance(event.getStartingBalance());
            gp.setInviteToken(ep.getInviteToken());
            gp.setJoinToken(ep.getJoinToken());
            gamePlayerMapper.insert(gp);
            gamePlayerMapper.markJoined(gp.getId());

            if (ep.getDeviceToken() != null && !ep.getDeviceToken().isBlank()) {
                gamePlayerMapper.updateDeviceToken(gp.getId(), ep.getDeviceToken());
            }
        }

        eventMapper.updateStatus(event.getId(), "active");
    }

    @Transactional
    public void completeEvent(Event event) {
        if (!"active".equals(event.getStatus())) return;

        List<Game> games = gameMapper.findByEventId(event.getId());
        for (Game game : games) {
            gameService.completeGame(game);
        }

        eventMapper.updateStatus(event.getId(), "completed");
    }
}
