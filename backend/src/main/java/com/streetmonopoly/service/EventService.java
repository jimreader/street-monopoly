package com.streetmonopoly.service;

import com.streetmonopoly.dto.Dtos.*;
import com.streetmonopoly.mapper.*;
import com.streetmonopoly.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class EventService {

    @Autowired private EventMapper eventMapper;
    @Autowired private EventPlayerMapper eventPlayerMapper;
    @Autowired private EventChallengeMapper eventChallengeMapper;
    @Autowired private EventChallengeSubmissionMapper eventChallengeSubmissionMapper;
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
        event.setLogoImageUrl(normalizeOptionalText(request.getLogoImageUrl()));
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
    public Event updateEvent(UUID eventId, UpdateEventRequest request) {
        Event existing = getEvent(eventId);
        if (!"pending".equals(existing.getStatus())) {
            throw new RuntimeException("Only pending events can be edited");
        }

        GameMap map = gameMapMapper.findById(request.getGameMapId());
        if (map == null) throw new RuntimeException("Game map not found");

        if (request.getEndTime().isBefore(request.getStartTime()) || request.getEndTime().isEqual(request.getStartTime())) {
            throw new RuntimeException("End time must be after start time");
        }

        existing.setName(request.getName().trim());
        existing.setLogoImageUrl(normalizeOptionalText(request.getLogoImageUrl()));
        existing.setGameMapId(request.getGameMapId());
        existing.setStartTime(request.getStartTime());
        existing.setEndTime(request.getEndTime());
        existing.setStartingBalance(request.getStartingBalance());
        existing.setProximityMetres(request.getProximityMetres());
        existing.setMaxPlayersPerGame(request.getMaxPlayersPerGame());

        int updated = eventMapper.updateDetails(existing);
        if (updated == 0) throw new RuntimeException("Event not found: " + eventId);
        return eventMapper.findById(eventId);
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

    public List<ChallengeAdminView> getEventChallenges(UUID eventId) {
        getEvent(eventId);
        return eventChallengeMapper.findAdminViewsByEventId(eventId);
    }

    @Transactional
    public EventChallenge createChallenge(UUID eventId, CreateChallengeRequest request) {
        Event event = getEvent(eventId);
        validateChallengeEditableWindow(event);

        int totalDurationMinutes = eventChallengeMapper.sumDurationsByEvent(eventId) + request.getDurationMinutes();
        validateChallengeDurationsFitEvent(event, totalDurationMinutes);

        EventChallenge challenge = new EventChallenge();
        challenge.setId(UUID.randomUUID());
        challenge.setEventId(eventId);
        challenge.setDescription(request.getDescription().trim());
        challenge.setPrizeAmount(request.getPrizeAmount());
        challenge.setDurationMinutes(request.getDurationMinutes());
        challenge.setStatus("pending");
        eventChallengeMapper.insert(challenge);
        return eventChallengeMapper.findByEventAndId(eventId, challenge.getId());
    }

    @Transactional
    public EventChallenge updateChallenge(UUID eventId, UUID challengeId, UpdateChallengeRequest request) {
        Event event = getEvent(eventId);
        validateChallengeEditableWindow(event);

        EventChallenge existing = eventChallengeMapper.findByEventAndId(eventId, challengeId);
        if (existing == null) throw new RuntimeException("Challenge not found");
        if (!"pending".equals(existing.getStatus())) {
            throw new RuntimeException("Only pending challenges can be edited");
        }

        int totalDurationMinutes = eventChallengeMapper.sumDurationsByEventExcluding(eventId, challengeId) + request.getDurationMinutes();
        validateChallengeDurationsFitEvent(event, totalDurationMinutes);

        existing.setDescription(request.getDescription().trim());
        existing.setPrizeAmount(request.getPrizeAmount());
        existing.setDurationMinutes(request.getDurationMinutes());
        int updated = eventChallengeMapper.updateDraft(existing);
        if (updated == 0) throw new RuntimeException("Challenge not found");
        return eventChallengeMapper.findByEventAndId(eventId, challengeId);
    }

    @Transactional
    public void deleteChallenge(UUID eventId, UUID challengeId) {
        Event event = getEvent(eventId);
        validateChallengeEditableWindow(event);

        EventChallenge existing = eventChallengeMapper.findByEventAndId(eventId, challengeId);
        if (existing == null) throw new RuntimeException("Challenge not found");
        if (!"pending".equals(existing.getStatus())) {
            throw new RuntimeException("Only pending challenges can be deleted");
        }

        int updated = eventChallengeMapper.softDelete(eventId, challengeId);
        if (updated == 0) throw new RuntimeException("Challenge not found");
    }

    public List<ChallengeSubmissionAdminView> getChallengeSubmissions(UUID eventId, UUID challengeId) {
        getEvent(eventId);
        EventChallenge challenge = eventChallengeMapper.findByEventAndId(eventId, challengeId);
        if (challenge == null) throw new RuntimeException("Challenge not found");
        return eventChallengeSubmissionMapper.findAdminSubmissions(eventId, challengeId);
    }

    @Transactional
    public void reviewChallengeSubmission(UUID eventId,
                                          UUID challengeId,
                                          UUID submissionId,
                                          ReviewChallengeSubmissionRequest request) {
        Event event = getEvent(eventId);
        if ("pending".equals(event.getStatus())) {
            throw new RuntimeException("Challenge submissions can only be reviewed once the event has started");
        }

        EventChallenge challenge = eventChallengeMapper.findByEventAndId(eventId, challengeId);
        if (challenge == null) throw new RuntimeException("Challenge not found");

        EventChallengeSubmission submission = eventChallengeSubmissionMapper.findInEvent(eventId, challengeId, submissionId);
        if (submission == null) throw new RuntimeException("Submission not found");
        if (!"pending".equals(submission.getReviewStatus())) {
            throw new RuntimeException("Submission has already been reviewed");
        }

        BigDecimal prizeAward = BigDecimal.ZERO;
        if ("accomplished".equals(request.getReviewStatus())) {
            GamePlayer gp = gamePlayerMapper.findByEventPlayerId(submission.getEventPlayerId());
            if (gp == null) throw new RuntimeException("Player is not assigned to a game");

            BigDecimal newBalance = gp.getBalance().add(challenge.getPrizeAmount());
            gamePlayerMapper.updateBalance(gp.getId(), newBalance);
            prizeAward = challenge.getPrizeAmount();

            // Balance updates alone don't move a completed game's leaderboard, which ranks by finalBalance
            Game game = gameMapper.findById(gp.getGameId());
            if (game != null && "completed".equals(game.getStatus()) && gp.getFinalBalance() != null) {
                gamePlayerMapper.updateFinalBalance(gp.getId(), gp.getFinalBalance().add(challenge.getPrizeAmount()));
            }
        }

        eventChallengeSubmissionMapper.review(
                submission.getId(),
                request.getReviewStatus(),
                request.getReviewNotes(),
                LocalDateTime.now(),
                prizeAward
        );
    }

    public AdminEventView getAdminEventView(UUID eventId) {
        Event event = getEvent(eventId);
        GameMap map = gameMapMapper.findById(event.getGameMapId());
        List<Game> games = gameMapper.findByEventId(eventId);
        List<EventPlayer> eventPlayers = eventPlayerMapper.findByEventId(eventId);

        AdminEventView view = new AdminEventView();
        view.setEventId(event.getId());
        view.setEventName(event.getName());
        view.setLogoImageUrl(event.getLogoImageUrl());
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

    @Scheduled(fixedRateString = "${app.events.status-update-ms:3000}")
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

        updateChallengeStatuses();
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

        scheduleEventChallenges(event);

        eventMapper.updateStatus(event.getId(), "active");
    }

    @Transactional
    public void completeEvent(Event event) {
        if (!"active".equals(event.getStatus())) return;

        List<Game> games = gameMapper.findByEventId(event.getId());
        for (Game game : games) {
            gameService.completeGame(game);
        }

        List<EventChallenge> challenges = eventChallengeMapper.findByEventId(event.getId());
        for (EventChallenge challenge : challenges) {
            if (!"completed".equals(challenge.getStatus())) {
                eventChallengeMapper.updateStatus(challenge.getId(), "completed");
            }
        }

        eventMapper.updateStatus(event.getId(), "completed");
    }

    private void validateChallengeEditableWindow(Event event) {
        if (!"pending".equals(event.getStatus())) {
            throw new RuntimeException("Challenges can only be managed while an event is pending");
        }
        if (!LocalDateTime.now().isBefore(event.getStartTime())) {
            throw new RuntimeException("Challenges can only be managed before the event starts");
        }
    }

    private String normalizeOptionalText(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateChallengeDurationsFitEvent(Event event, int totalDurationMinutes) {
        long eventWindowSeconds = Duration.between(event.getStartTime(), event.getEndTime()).getSeconds();
        long challengeWindowSeconds = totalDurationMinutes * 60L;
        if (challengeWindowSeconds > eventWindowSeconds) {
            throw new RuntimeException("Total challenge durations exceed the event duration");
        }
    }

    private void scheduleEventChallenges(Event event) {
        List<EventChallenge> challenges = eventChallengeMapper.findByEventId(event.getId());
        if (challenges.isEmpty()) return;

        long eventWindowSeconds = Duration.between(event.getStartTime(), event.getEndTime()).getSeconds();
        long challengeSeconds = 0;
        for (EventChallenge challenge : challenges) {
            challengeSeconds += challenge.getDurationMinutes() * 60L;
        }

        if (challengeSeconds > eventWindowSeconds) {
            throw new RuntimeException("Configured challenge durations exceed the event duration");
        }

        List<EventChallenge> shuffled = new ArrayList<>(challenges);
        Collections.shuffle(shuffled);

        long slackSeconds = eventWindowSeconds - challengeSeconds;
        long[] gaps = distributeRandomGapSeconds(slackSeconds, shuffled.size() + 1);

        LocalDateTime cursor = event.getStartTime().plusSeconds(gaps[0]);
        for (int i = 0; i < shuffled.size(); i++) {
            EventChallenge challenge = shuffled.get(i);
            LocalDateTime startAt = cursor;
            LocalDateTime endAt = startAt.plusMinutes(challenge.getDurationMinutes());
            eventChallengeMapper.setSchedule(challenge.getId(), startAt, endAt);
            eventChallengeMapper.updateStatus(challenge.getId(), "pending");
            cursor = endAt.plusSeconds(gaps[i + 1]);
        }
    }

    private long[] distributeRandomGapSeconds(long slackSeconds, int bucketCount) {
        long[] gaps = new long[bucketCount];
        if (slackSeconds <= 0 || bucketCount <= 0) return gaps;

        Random random = new Random();
        double[] weights = new double[bucketCount];
        double totalWeight = 0;
        for (int i = 0; i < bucketCount; i++) {
            weights[i] = random.nextDouble();
            totalWeight += weights[i];
        }

        long assigned = 0;
        for (int i = 0; i < bucketCount; i++) {
            long value = (long) Math.floor((weights[i] / totalWeight) * slackSeconds);
            gaps[i] = value;
            assigned += value;
        }

        long remainder = slackSeconds - assigned;
        for (int i = 0; i < remainder; i++) {
            int index = random.nextInt(bucketCount);
            gaps[index] += 1;
        }

        return gaps;
    }

    private void updateChallengeStatuses() {
        List<EventChallenge> toActivate = eventChallengeMapper.findPendingReadyToActivate();
        for (EventChallenge challenge : toActivate) {
            Event event = eventMapper.findById(challenge.getEventId());
            if (event != null && "active".equals(event.getStatus())) {
                eventChallengeMapper.updateStatus(challenge.getId(), "active");
            }
        }

        List<EventChallenge> toComplete = eventChallengeMapper.findActiveReadyToComplete();
        for (EventChallenge challenge : toComplete) {
            eventChallengeMapper.updateStatus(challenge.getId(), "completed");
        }
    }
}
