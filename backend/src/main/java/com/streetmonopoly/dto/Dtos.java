package com.streetmonopoly.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class Dtos {

    // ---- Game Map ----
    @Data
    public static class CreateGameMapRequest {
        @NotBlank private String name;
        private String postcodeArea;
    }

    @Data
    public static class CreateStreetRequest {
        @NotBlank private String name;
        @NotNull @Positive private BigDecimal price;
        @NotNull @Positive private BigDecimal rentalPrice;
        @NotBlank private String colour;
        @NotNull private Double latitude;
        @NotNull private Double longitude;
        private String imageClueUrl;
    }

    // ---- Game ----
    @Data
    public static class CreateGameRequest {
        @NotBlank private String name;
        @NotNull private UUID gameMapId;
        @NotNull private LocalDateTime startTime;
        @NotNull private LocalDateTime endTime;
        @NotNull @Positive private BigDecimal startingBalance;
        @NotNull @Min(1) private Integer proximityMetres;
    }

    // ---- Event ----
    @Data
    public static class CreateEventRequest {
        @NotBlank private String name;
        @NotNull private UUID gameMapId;
        @NotNull private LocalDateTime startTime;
        @NotNull private LocalDateTime endTime;
        @NotNull @Positive private BigDecimal startingBalance;
        @NotNull @Min(1) private Integer proximityMetres;
        @NotNull @Min(1) private Integer maxPlayersPerGame;
        @NotNull @Min(0) private Integer unvisitedStreetPenaltyPercent;
        private String logoImageUrl;
    }

    @Data
    public static class UpdateEventRequest {
        @NotBlank private String name;
        @NotNull private UUID gameMapId;
        @NotNull private LocalDateTime startTime;
        @NotNull private LocalDateTime endTime;
        @NotNull @Positive private BigDecimal startingBalance;
        @NotNull @Min(1) private Integer proximityMetres;
        @NotNull @Min(1) private Integer maxPlayersPerGame;
        @NotNull @Min(0) private Integer unvisitedStreetPenaltyPercent;
        private String logoImageUrl;
    }

    // ---- Invite ----
    @Data
    public static class InvitePlayerRequest {
        @NotBlank private String name;
        @NotBlank @Email private String email;
    }

    // ---- Challenges ----
    @Data
    public static class CreateChallengeRequest {
        @NotBlank private String description;
        @NotNull @Positive private BigDecimal prizeAmount;
        @NotNull @Min(1) private Integer durationMinutes;
    }

    @Data
    public static class UpdateChallengeRequest {
        @NotBlank private String description;
        @NotNull @Positive private BigDecimal prizeAmount;
        @NotNull @Min(1) private Integer durationMinutes;
    }

    @Data
    public static class ChallengeSubmissionRequest {
        @NotBlank private String photoUrl;
    }

    @Data
    public static class ReviewChallengeSubmissionRequest {
        @NotBlank
        @Pattern(regexp = "accomplished|failed", message = "reviewStatus must be accomplished or failed")
        private String reviewStatus;
        private String reviewNotes;
    }

    @Data
    public static class ChallengeAdminView {
        private UUID id;
        private UUID eventId;
        private String description;
        private BigDecimal prizeAmount;
        private int durationMinutes;
        private LocalDateTime scheduledStartAt;
        private LocalDateTime scheduledEndAt;
        private String status;
        private int submittedCount;
        private int accomplishedCount;
        private int failedCount;
        private int pendingReviewCount;
        private LocalDateTime createdAt;
    }

    @Data
    public static class ChallengeSubmissionAdminView {
        private UUID submissionId;
        private UUID challengeId;
        private UUID eventPlayerId;
        private UUID playerId;
        private String playerName;
        private String playerEmail;
        private String photoUrl;
        private LocalDateTime submittedAt;
        private String reviewStatus;
        private String reviewNotes;
        private LocalDateTime reviewedAt;
        private BigDecimal prizeAwardedAmount;
    }

    @Data
    public static class PlayerChallengeView {
        private UUID id;
        private String description;
        private BigDecimal prizeAmount;
        private int durationMinutes;
        private LocalDateTime scheduledStartAt;
        private LocalDateTime scheduledEndAt;
        private String status;
        private String submissionStatus;
        private String submittedPhotoUrl;
        private LocalDateTime submittedAt;
        private String reviewStatus;
        private String reviewNotes;
        private LocalDateTime reviewedAt;
    }

    // ---- Check-in ----
    @Data
    public static class CheckInRequest {
        @NotNull private UUID streetId;
        @NotNull private Double latitude;
        @NotNull private Double longitude;
    }

    @Data
    public static class CheckInResponse {
        private String outcome; // purchased, rent_paid, insufficient_funds, too_far, already_visited, game_not_active
        private String message;
        private BigDecimal amount;
        private BigDecimal newBalance;
    }

    // ---- Player Game View ----
    @Data
    public static class PlayerJoinSummary {
        private UUID gameId;
        private String gameName;
        private String eventName;
        private String eventLogoUrl;
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }

    @Data
    public static class PlayerGameView {
        private UUID gameId;
        private String gameName;
        private String eventName;
        private String eventLogoUrl;
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private BigDecimal balance;
        private BigDecimal finalBalance;
        private int proximityMetres;
        private List<PlayerStreetView> streets;
        private List<RentCollection> rentCollections;
    }

    @Data
    public static class PlayerStreetView {
        private UUID streetId;
        private String name;
        private BigDecimal price;
        private BigDecimal rentalPrice;
        private String colour;
        private double latitude;
        private double longitude;
        private String imageClueUrl;
        private String visitStatus; // unvisited, owned, visited_rent, visited_no_funds
        private boolean ownedByPlayer;
    }

    // ---- Rent collection record ----
    @Data
    public static class RentCollection {
        private String streetName;
        private String streetColour;
        private String paidByPlayerName;
        private BigDecimal amount;
        private LocalDateTime collectedAt;
    }

    // ---- Admin Game View ----
    @Data
    public static class AdminGameView {
        private UUID gameId;
        private String gameName;
        private String eventName;
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private BigDecimal startingBalance;
        private int proximityMetres;
        private String mapName;
        private List<AdminStreetView> streets;
        private List<LeaderboardEntry> leaderboard;
        private List<PlayerLocation> playerLocations;
    }

    // ---- Admin Event View ----
    @Data
    public static class AdminEventView {
        private UUID eventId;
        private String eventName;
        private String logoImageUrl;
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private BigDecimal startingBalance;
        private int proximityMetres;
        private int maxPlayersPerGame;
        private int unvisitedStreetPenaltyPercent;
        private String mapName;
        private List<EventGameSummary> games;
        private List<LeaderboardEntry> leaderboard;
    }

    @Data
    public static class EventGameSummary {
        private UUID gameId;
        private String gameName;
        private String status;
        private int playerCount;
    }

    @Data
    public static class AdminStreetView {
        private UUID streetId;
        private String name;
        private BigDecimal price;
        private BigDecimal rentalPrice;
        private String colour;
        private String ownerName;
        private List<StreetVisitor> visitors;
    }

    @Data
    public static class StreetVisitor {
        private String playerName;
        private String visitType;
        private LocalDateTime visitedAt;
    }

    @Data
    public static class LeaderboardEntry {
        private UUID playerId;
        private String playerName;
        private BigDecimal balance;
        private BigDecimal finalBalance;
        private int streetsOwned;
        private int rank;
        private List<RentCollection> rentCollections;
    }

    @Data
    public static class PlayerLocation {
        private UUID playerId;
        private String playerName;
        private String streetName;
        private double latitude;
        private double longitude;
        private LocalDateTime visitedAt;
    }
}
