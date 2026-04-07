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
import java.util.stream.Collectors;

@Service
public class GameService {

    @Autowired private GameMapper gameMapper;
    @Autowired private GameMapMapper gameMapMapper;
    @Autowired private GamePlayerMapper gamePlayerMapper;
    @Autowired private GameStreetMapper gameStreetMapper;
    @Autowired private StreetVisitMapper streetVisitMapper;
    @Autowired private PlayerMapper playerMapper;
    @Autowired private EmailService emailService;

    public List<Game> getAllGames() {
        return gameMapper.findAll();
    }

    public Game getGame(UUID id) {
        Game game = gameMapper.findById(id);
        if (game == null) throw new RuntimeException("Game not found: " + id);
        return game;
    }

    @Transactional
    public Game createGame(CreateGameRequest request) {
        GameMap map = gameMapMapper.findById(request.getGameMapId());
        if (map == null) throw new RuntimeException("Game map not found");

        Game game = new Game();
        game.setId(UUID.randomUUID());
        game.setName(request.getName());
        game.setGameMapId(request.getGameMapId());
        game.setStartTime(request.getStartTime());
        game.setEndTime(request.getEndTime());
        game.setStartingBalance(request.getStartingBalance());
        game.setProximityMetres(request.getProximityMetres());
        game.setStatus("pending");
        gameMapper.insert(game);

        // Create game_street entries for all streets in the map
        List<Street> streets = gameMapMapper.findStreetsByMapId(request.getGameMapId());
        for (Street street : streets) {
            GameStreet gs = new GameStreet();
            gs.setId(UUID.randomUUID());
            gs.setGameId(game.getId());
            gs.setStreetId(street.getId());
            gameStreetMapper.insert(gs);
        }

        return gameMapper.findById(game.getId());
    }

    @Transactional
    public GamePlayer invitePlayer(UUID gameId, InvitePlayerRequest request) {
        Game game = getGame(gameId);

        // Find or create player
        Player player = playerMapper.findByEmail(request.getEmail());
        if (player == null) {
            player = new Player();
            player.setId(UUID.randomUUID());
            player.setName(request.getName());
            player.setEmail(request.getEmail());
            playerMapper.insert(player);
        } else {
            player.setName(request.getName());
            playerMapper.update(player);
        }

        // Check if already invited
        GamePlayer existing = gamePlayerMapper.findByGameAndPlayer(gameId, player.getId());
        if (existing != null) {
            throw new RuntimeException("Player already invited to this game");
        }

        // Create the game-player link — immediately joined, no acceptance step
        GamePlayer gp = new GamePlayer();
        gp.setId(UUID.randomUUID());
        gp.setGameId(gameId);
        gp.setPlayerId(player.getId());
        gp.setBalance(game.getStartingBalance());
        gp.setInviteToken(UUID.randomUUID());
        gp.setJoinToken(UUID.randomUUID());
        gamePlayerMapper.insert(gp);
        gamePlayerMapper.markJoined(gp.getId());

        // Send them the game link directly — one click to play
        emailService.sendJoinEmail(player.getEmail(), player.getName(), game.getName(), gp.getJoinToken());

        gp.setPlayer(player);
        return gp;
    }

    public PlayerGameView getPlayerView(UUID joinToken, String deviceToken) {
        GamePlayer gp = gamePlayerMapper.findByJoinToken(joinToken);
        if (gp == null) throw new RuntimeException("Invalid join token");

        validateAndBindDevice(gp, deviceToken);

        Game game = gameMapper.findById(gp.getGameId());
        List<GameStreet> gameStreets = gameStreetMapper.findByGameId(game.getId());
        List<StreetVisit> playerVisits = streetVisitMapper.findByGameAndPlayer(game.getId(), gp.getPlayerId());
        Map<UUID, StreetVisit> visitMap = playerVisits.stream()
                .collect(Collectors.toMap(StreetVisit::getStreetId, v -> v));

        PlayerGameView view = new PlayerGameView();
        view.setGameId(game.getId());
        view.setGameName(game.getName());
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

    @Transactional
    public CheckInResponse checkIn(UUID joinToken, String deviceToken, CheckInRequest request) {
        GamePlayer gp = gamePlayerMapper.findByJoinToken(joinToken);
        if (gp == null) throw new RuntimeException("Invalid join token");

        validateAndBindDevice(gp, deviceToken);

        Game game = gameMapper.findById(gp.getGameId());
        CheckInResponse response = new CheckInResponse();

        if (!"active".equals(game.getStatus())) {
            response.setOutcome("game_not_active");
            response.setMessage("The game is not currently active.");
            response.setNewBalance(gp.getBalance());
            return response;
        }

        // Check if already visited
        StreetVisit existingVisit = streetVisitMapper.findByGameStreetAndPlayer(
                game.getId(), request.getStreetId(), gp.getPlayerId());
        if (existingVisit != null) {
            response.setOutcome("already_visited");
            response.setMessage("You have already visited this street.");
            response.setNewBalance(gp.getBalance());
            return response;
        }

        // Check proximity
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

        // Check ownership
        GameStreet gameStreet = gameStreetMapper.findByGameAndStreet(game.getId(), request.getStreetId());

        if (gameStreet.getOwnerPlayerId() == null) {
            // Street not owned - try to purchase
            if (gp.getBalance().compareTo(street.getPrice()) >= 0) {
                // Purchase
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
                // Insufficient funds - mark as visited
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
            // Player owns this street already
            response.setOutcome("already_visited");
            response.setMessage("You already own this street!");
            response.setNewBalance(gp.getBalance());
        } else {
            // Owned by another player - pay rent
            BigDecimal rent = street.getRentalPrice();
            BigDecimal newBalance = gp.getBalance().subtract(rent);
            gamePlayerMapper.updateBalance(gp.getId(), newBalance);

            // Add rent to owner's balance
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
        GameMap map = gameMapMapper.findById(game.getGameMapId());
        List<GameStreet> gameStreets = gameStreetMapper.findByGameId(gameId);
        List<GamePlayer> gamePlayers = gamePlayerMapper.findAllByGameId(gameId);

        AdminGameView view = new AdminGameView();
        view.setGameId(game.getId());
        view.setGameName(game.getName());
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

            // Populate visitors for this street
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

        // Sort by final_balance (completed) or balance (active)
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

        // Build player last-known locations from most recent check-ins
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

    /**
     * Scheduled task to activate pending games when their start time arrives
     * and complete active games when their end time passes.
     */
    @Scheduled(fixedRate = 15000) // every 15 seconds
    @Transactional
    public void updateGameStatuses() {
        // Activate pending games
        List<Game> toActivate = gameMapper.findPendingReadyToStart();
        for (Game game : toActivate) {
            gameMapper.updateStatus(game.getId(), "active");
            System.out.println("Game activated: " + game.getName());
        }

        // Complete active games
        List<Game> toComplete = gameMapper.findActiveReadyToEnd();
        for (Game game : toComplete) {
            completeGame(game);
        }
    }

    @Transactional
    public void completeGame(Game game) {
        gameMapper.updateStatus(game.getId(), "completed");
        List<GamePlayer> players = gamePlayerMapper.findAllByGameId(game.getId());
        List<GameStreet> allStreets = gameStreetMapper.findByGameId(game.getId());

        for (GamePlayer gp : players) {
            BigDecimal finalBalance = gp.getBalance();

            // Subtract rental value for unvisited streets
            for (GameStreet gs : allStreets) {
                // Skip streets the player owns
                if (gp.getPlayerId().equals(gs.getOwnerPlayerId())) continue;

                StreetVisit visit = streetVisitMapper.findByGameStreetAndPlayer(
                        game.getId(), gs.getStreetId(), gp.getPlayerId());
                if (visit == null) {
                    // Unvisited street - subtract rental value
                    Street street = gameMapMapper.findStreetById(gs.getStreetId());
                    finalBalance = finalBalance.subtract(street.getRentalPrice());
                }
            }

            gamePlayerMapper.updateFinalBalance(gp.getId(), finalBalance);
        }
        System.out.println("Game completed: " + game.getName());
    }

    public List<GamePlayer> getGamePlayers(UUID gameId) {
        return gamePlayerMapper.findByGameId(gameId);
    }

    /**
     * Bind a player to a single device. On the first request that includes a device token,
     * the token is stored. Subsequent requests from a different device are rejected.
     */
    private void validateAndBindDevice(GamePlayer gp, String deviceToken) {
        if (deviceToken == null || deviceToken.isBlank()) return; // no token provided — allow (backwards compat)

        if (gp.getDeviceToken() == null) {
            // First device to connect — bind it
            gamePlayerMapper.updateDeviceToken(gp.getId(), deviceToken);
            gp.setDeviceToken(deviceToken);
        } else if (!gp.getDeviceToken().equals(deviceToken)) {
            throw new RuntimeException("This game can only be played from the device you originally joined on.");
        }
    }

    /**
     * Calculate distance between two GPS coordinates using the Haversine formula.
     * Returns distance in metres.
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Earth's radius in metres
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Build rent collection records for a given property owner.
     * Shows which of their streets have earned rent, who paid it, and when.
     */
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
