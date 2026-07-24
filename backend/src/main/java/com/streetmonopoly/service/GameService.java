package com.streetmonopoly.service;

import com.streetmonopoly.dto.Dtos.*;
import com.streetmonopoly.mapper.*;
import com.streetmonopoly.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class GameService {

    @Autowired private GameMapper gameMapper;
    @Autowired private EventMapper eventMapper;
    @Autowired private EventPlayerMapper eventPlayerMapper;
    @Autowired private GameMapMapper gameMapMapper;
    @Autowired private GamePlayerMapper gamePlayerMapper;
    @Autowired private GameStreetMapper gameStreetMapper;
    @Autowired private StreetVisitMapper streetVisitMapper;
    @Autowired private PlayerMapper playerMapper;

    public Game getGame(UUID id) {
        Game game = gameMapper.findById(id);
        if (game == null) throw new RuntimeException("Game not found: " + id);
        return game;
    }

    public PlayerGameView getPlayerView(UUID joinToken, String deviceToken) {
        GamePlayer gp = gamePlayerMapper.findByJoinToken(joinToken);
        if (gp == null) {
            EventPlayer ep = eventPlayerMapper.findByJoinToken(joinToken);
            if (ep == null) throw new RuntimeException("Invalid join token");

            Event event = eventMapper.findById(ep.getEventId());
            if (event == null) throw new RuntimeException("Event not found");

            validateAndBindDevice(ep, deviceToken);

            PlayerGameView pendingView = new PlayerGameView();
            pendingView.setGameId(ep.getAssignedGameId());
            pendingView.setGameName(event.getName());
            pendingView.setEventName(event.getName());
            pendingView.setStatus(event.getStatus());
            pendingView.setStartTime(event.getStartTime());
            pendingView.setEndTime(event.getEndTime());
            pendingView.setBalance(event.getStartingBalance());
            pendingView.setFinalBalance(null);
            pendingView.setProximityMetres(event.getProximityMetres());
            pendingView.setStreets(Collections.emptyList());
            pendingView.setRentCollections(Collections.emptyList());
            return pendingView;
        }

        validateAndBindDevice(gp, deviceToken);

        Game game = gameMapper.findById(gp.getGameId());
        Event event = game.getEventId() == null ? null : eventMapper.findById(game.getEventId());
        List<GameStreet> gameStreets = gameStreetMapper.findByGameId(game.getId());
        List<StreetVisit> playerVisits = streetVisitMapper.findByGameAndPlayer(game.getId(), gp.getPlayerId());
        Map<UUID, StreetVisit> visitMap = new HashMap<>();
        for (StreetVisit visit : playerVisits) {
            visitMap.put(visit.getStreetId(), visit);
        }

        PlayerGameView view = new PlayerGameView();
        view.setGameId(game.getId());
        view.setGameName(game.getName());
        view.setEventName(event != null ? event.getName() : game.getName());
        view.setStatus(game.getStatus());
        view.setStartTime(game.getStartTime());
        view.setEndTime(game.getEndTime());
        view.setBalance(gp.getBalance());
        view.setFinalBalance(gp.getFinalBalance());
        view.setProximityMetres(game.getProximityMetres());

        List<PlayerStreetView> streetViews = new ArrayList<>();
        for (GameStreet gs : gameStreets) {
            Street street = gameMapMapper.findStreetById(gs.getStreetId());
            PlayerStreetView sv = new PlayerStreetView();
            sv.setStreetId(street.getId());
            sv.setName(street.getName());
            sv.setPrice(street.getPrice());
            sv.setRentalPrice(street.getRentalPrice());
            sv.setColour(street.getColour());
            sv.setLatitude(street.getLatitude());
            sv.setLongitude(street.getLongitude());
            sv.setImageClueUrl(street.getImageClueUrl());

            boolean ownedByPlayer = gp.getPlayerId().equals(gs.getOwnerPlayerId());
            sv.setOwnedByPlayer(ownedByPlayer);

            StreetVisit visit = visitMap.get(street.getId());
            if (ownedByPlayer) {
                sv.setVisitStatus("owned");
            } else if (visit != null) {
                sv.setVisitStatus("visited_" + visit.getVisitType());
            } else {
                sv.setVisitStatus("unvisited");
            }

            streetViews.add(sv);
        }
        view.setStreets(streetViews);
        view.setRentCollections(buildRentCollections(game.getId(), gp.getPlayerId()));
        return view;
    }

    public PlayerJoinSummary getPlayerJoinSummary(UUID joinToken) {
        GamePlayer gp = gamePlayerMapper.findByJoinToken(joinToken);
        if (gp != null) {
            Game game = gameMapper.findById(gp.getGameId());
            if (game == null) throw new RuntimeException("Game not found");
            Event event = game.getEventId() == null ? null : eventMapper.findById(game.getEventId());

            PlayerJoinSummary summary = new PlayerJoinSummary();
            summary.setGameId(game.getId());
            summary.setGameName(game.getName());
            summary.setEventName(event != null ? event.getName() : game.getName());
            summary.setStatus(game.getStatus());
            summary.setStartTime(game.getStartTime());
            summary.setEndTime(game.getEndTime());
            return summary;
        }

        EventPlayer ep = eventPlayerMapper.findByJoinToken(joinToken);
        if (ep == null) throw new RuntimeException("Invalid join token");

        Event event = eventMapper.findById(ep.getEventId());
        if (event == null) throw new RuntimeException("Event not found");

        PlayerJoinSummary summary = new PlayerJoinSummary();
        summary.setGameId(ep.getAssignedGameId());
        summary.setGameName(event.getName());
        summary.setEventName(event.getName());
        summary.setStatus(event.getStatus());
        summary.setStartTime(event.getStartTime());
        summary.setEndTime(event.getEndTime());
        return summary;
    }

    @Transactional
    public CheckInResponse checkIn(UUID joinToken, String deviceToken, CheckInRequest request) {
        GamePlayer gp = gamePlayerMapper.findByJoinToken(joinToken);
        if (gp == null) {
            EventPlayer ep = eventPlayerMapper.findByJoinToken(joinToken);
            if (ep != null) {
                throw new RuntimeException("This event has not assigned you to a game yet.");
            }
            throw new RuntimeException("Invalid join token");
        }

        validateAndBindDevice(gp, deviceToken);

        Game game = gameMapper.findById(gp.getGameId());
        CheckInResponse response = new CheckInResponse();

        if (!"active".equals(game.getStatus())) {
            response.setOutcome("game_not_active");
            response.setMessage("The game is not currently active.");
            response.setNewBalance(gp.getBalance());
            return response;
        }

        StreetVisit existingVisit = streetVisitMapper.findByGameStreetAndPlayer(
                game.getId(), request.getStreetId(), gp.getPlayerId());
        if (existingVisit != null) {
            response.setOutcome("already_visited");
            response.setMessage("You have already visited this street.");
            response.setNewBalance(gp.getBalance());
            return response;
        }

        Street street = gameMapMapper.findStreetById(request.getStreetId());
        if (street == null) throw new RuntimeException("Street not found");

        double distance = calculateDistance(
                request.getLatitude(), request.getLongitude(),
                street.getLatitude(), street.getLongitude());

        if (distance > game.getProximityMetres()) {
            response.setOutcome("too_far");
            response.setMessage(String.format("You are %.0fm away. You need to be within %dm.", distance, game.getProximityMetres()));
            response.setNewBalance(gp.getBalance());
            return response;
        }

        GameStreet gameStreet = gameStreetMapper.findByGameAndStreet(game.getId(), request.getStreetId());

        if (gameStreet.getOwnerPlayerId() == null) {
            if (gp.getBalance().compareTo(street.getPrice()) >= 0) {
                BigDecimal newBalance = gp.getBalance().subtract(street.getPrice());
                gamePlayerMapper.updateBalance(gp.getId(), newBalance);
                gameStreetMapper.updateOwner(gameStreet.getId(), gp.getPlayerId());

                StreetVisit visit = new StreetVisit();
                visit.setId(UUID.randomUUID());
                visit.setGameId(game.getId());
                visit.setStreetId(street.getId());
                visit.setPlayerId(gp.getPlayerId());
                visit.setVisitType("purchased");
                visit.setAmount(street.getPrice());
                streetVisitMapper.insert(visit);

                response.setOutcome("purchased");
                response.setMessage("You purchased " + street.getName() + "!");
                response.setAmount(street.getPrice());
                response.setNewBalance(newBalance);
            } else {
                StreetVisit visit = new StreetVisit();
                visit.setId(UUID.randomUUID());
                visit.setGameId(game.getId());
                visit.setStreetId(street.getId());
                visit.setPlayerId(gp.getPlayerId());
                visit.setVisitType("insufficient_funds");
                visit.setAmount(BigDecimal.ZERO);
                streetVisitMapper.insert(visit);

                response.setOutcome("insufficient_funds");
                response.setMessage("Insufficient funds to purchase " + street.getName() + ". Street marked as visited.");
                response.setAmount(BigDecimal.ZERO);
                response.setNewBalance(gp.getBalance());
            }
        } else if (gameStreet.getOwnerPlayerId().equals(gp.getPlayerId())) {
            response.setOutcome("already_visited");
            response.setMessage("You already own this street!");
            response.setNewBalance(gp.getBalance());
        } else {
            BigDecimal rent = street.getRentalPrice();
            BigDecimal newBalance = gp.getBalance().subtract(rent);
            gamePlayerMapper.updateBalance(gp.getId(), newBalance);

            GamePlayer owner = gamePlayerMapper.findByGameAndPlayer(game.getId(), gameStreet.getOwnerPlayerId());
            BigDecimal ownerNewBalance = owner.getBalance().add(rent);
            gamePlayerMapper.updateBalance(owner.getId(), ownerNewBalance);

            StreetVisit visit = new StreetVisit();
            visit.setId(UUID.randomUUID());
            visit.setGameId(game.getId());
            visit.setStreetId(street.getId());
            visit.setPlayerId(gp.getPlayerId());
            visit.setVisitType("rent_paid");
            visit.setAmount(rent);
            streetVisitMapper.insert(visit);

            response.setOutcome("rent_paid");
            response.setMessage("This street is owned by another player. You paid £" + rent + " rent.");
            response.setAmount(rent);
            response.setNewBalance(newBalance);
        }

        return response;
    }

    public AdminGameView getAdminView(UUID gameId) {
        Game game = getGame(gameId);
        Event event = game.getEventId() == null ? null : eventMapper.findById(game.getEventId());
        GameMap map = gameMapMapper.findById(game.getGameMapId());
        List<GameStreet> gameStreets = gameStreetMapper.findByGameId(gameId);
        List<GamePlayer> gamePlayers = gamePlayerMapper.findAllByGameId(gameId);

        AdminGameView view = new AdminGameView();
        view.setGameId(game.getId());
        view.setGameName(game.getName());
        view.setEventName(event != null ? event.getName() : game.getName());
        view.setStatus(game.getStatus());
        view.setStartTime(game.getStartTime());
        view.setEndTime(game.getEndTime());
        view.setStartingBalance(game.getStartingBalance());
        view.setProximityMetres(game.getProximityMetres());
        view.setMapName(map.getName());

        List<AdminStreetView> streetViews = new ArrayList<>();
        for (GameStreet gs : gameStreets) {
            Street street = gameMapMapper.findStreetById(gs.getStreetId());
            AdminStreetView sv = new AdminStreetView();
            sv.setStreetId(street.getId());
            sv.setName(street.getName());
            sv.setPrice(street.getPrice());
            sv.setRentalPrice(street.getRentalPrice());
            sv.setColour(street.getColour());
            if (gs.getOwnerPlayerId() != null) {
                Player owner = playerMapper.findById(gs.getOwnerPlayerId());
                sv.setOwnerName(owner != null ? owner.getName() : null);
            }

            List<StreetVisitor> visitors = new ArrayList<>();
            List<StreetVisit> visits = streetVisitMapper.findByGameAndStreet(gameId, street.getId());
            for (StreetVisit visit : visits) {
                Player visitPlayer = playerMapper.findById(visit.getPlayerId());
                if (visitPlayer != null) {
                    StreetVisitor visitor = new StreetVisitor();
                    visitor.setPlayerName(visitPlayer.getName());
                    visitor.setVisitType(visit.getVisitType());
                    visitor.setVisitedAt(visit.getVisitedAt());
                    visitors.add(visitor);
                }
            }
            sv.setVisitors(visitors);
            streetViews.add(sv);
        }
        view.setStreets(streetViews);

        List<LeaderboardEntry> leaderboard = new ArrayList<>();
        for (GamePlayer gp : gamePlayers) {
            Player player = playerMapper.findById(gp.getPlayerId());
            LeaderboardEntry entry = new LeaderboardEntry();
            entry.setPlayerId(player.getId());
            entry.setPlayerName(player.getName());
            entry.setBalance(gp.getBalance());
            entry.setFinalBalance(gp.getFinalBalance());
            entry.setStreetsOwned(gameStreetMapper.countOwnedByPlayer(gameId, player.getId()));
            entry.setRentCollections(buildRentCollections(gameId, player.getId()));
            leaderboard.add(entry);
        }

        leaderboard.sort((a, b) -> {
            BigDecimal balA = "completed".equals(game.getStatus()) && a.getFinalBalance() != null
                    ? a.getFinalBalance() : a.getBalance();
            BigDecimal balB = "completed".equals(game.getStatus()) && b.getFinalBalance() != null
                    ? b.getFinalBalance() : b.getBalance();
            return balB.compareTo(balA);
        });

        for (int i = 0; i < leaderboard.size(); i++) {
            leaderboard.get(i).setRank(i + 1);
        }
        view.setLeaderboard(leaderboard);

        List<PlayerLocation> playerLocations = new ArrayList<>();
        List<StreetVisit> latestVisits = streetVisitMapper.findLatestVisitPerPlayer(gameId);
        for (StreetVisit visit : latestVisits) {
            Player visitPlayer = playerMapper.findById(visit.getPlayerId());
            Street visitStreet = gameMapMapper.findStreetById(visit.getStreetId());
            if (visitPlayer != null && visitStreet != null) {
                PlayerLocation loc = new PlayerLocation();
                loc.setPlayerId(visitPlayer.getId());
                loc.setPlayerName(visitPlayer.getName());
                loc.setStreetName(visitStreet.getName());
                loc.setLatitude(visitStreet.getLatitude());
                loc.setLongitude(visitStreet.getLongitude());
                loc.setVisitedAt(visit.getVisitedAt());
                playerLocations.add(loc);
            }
        }
        view.setPlayerLocations(playerLocations);

        return view;
    }

    public List<GamePlayer> getGamePlayers(UUID gameId) {
        return gamePlayerMapper.findByGameId(gameId);
    }

    public void resetPlayerDevice(UUID gameId, UUID gamePlayerId) {
        GamePlayer gp = gamePlayerMapper.findByGameAndId(gameId, gamePlayerId);
        if (gp == null) throw new RuntimeException("Player not found in this game");
        gamePlayerMapper.clearDeviceToken(gamePlayerId);
    }

    @Transactional
    public void removePlayer(UUID gameId, UUID gamePlayerId) {
        Game game = getGame(gameId);
        if ("completed".equals(game.getStatus())) {
            throw new RuntimeException("Cannot remove players from a completed game");
        }

        GamePlayer gp = gamePlayerMapper.findByGameAndId(gameId, gamePlayerId);
        if (gp == null) throw new RuntimeException("Player not found in this game");

        gameStreetMapper.clearOwnerByGameAndPlayer(gameId, gp.getPlayerId());
        int updated = gamePlayerMapper.softDelete(gameId, gamePlayerId);
        if (updated == 0) {
            throw new RuntimeException("Player not found in this game");
        }
    }

    @Transactional
    public void completeGame(Game game) {
        if ("completed".equals(game.getStatus())) return;

        gameMapper.updateStatus(game.getId(), "completed");
        List<GamePlayer> players = gamePlayerMapper.findAllByGameId(game.getId());
        List<GameStreet> allStreets = gameStreetMapper.findByGameId(game.getId());

        for (GamePlayer gp : players) {
            BigDecimal finalBalance = gp.getBalance();

            for (GameStreet gs : allStreets) {
                if (gp.getPlayerId().equals(gs.getOwnerPlayerId())) continue;

                StreetVisit visit = streetVisitMapper.findByGameStreetAndPlayer(
                        game.getId(), gs.getStreetId(), gp.getPlayerId());
                if (visit == null) {
                    Street street = gameMapMapper.findStreetById(gs.getStreetId());
                    finalBalance = finalBalance.subtract(street.getRentalPrice());
                }
            }

            gamePlayerMapper.updateFinalBalance(gp.getId(), finalBalance);
        }
    }

    private void validateAndBindDevice(GamePlayer gp, String deviceToken) {
        if (deviceToken == null || deviceToken.isBlank()) return;

        if (gp.getDeviceToken() == null) {
            gamePlayerMapper.updateDeviceToken(gp.getId(), deviceToken);
            gp.setDeviceToken(deviceToken);
        } else if (!gp.getDeviceToken().equals(deviceToken)) {
            throw new RuntimeException("This event can only be played from the device you originally joined on.");
        }
    }

    private void validateAndBindDevice(EventPlayer ep, String deviceToken) {
        if (deviceToken == null || deviceToken.isBlank()) return;

        if (ep.getDeviceToken() == null) {
            eventPlayerMapper.updateDeviceToken(ep.getId(), deviceToken);
            ep.setDeviceToken(deviceToken);
        } else if (!ep.getDeviceToken().equals(deviceToken)) {
            throw new RuntimeException("This event can only be played from the device you originally joined on.");
        }
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private List<RentCollection> buildRentCollections(UUID gameId, UUID ownerPlayerId) {
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
}
