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

    // ---- Invite ----
    @Data
    public static class InvitePlayerRequest {
        @NotBlank private String name;
        @NotBlank @Email private String email;
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
    public static class PlayerGameView {
        private UUID gameId;
        private String gameName;
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private BigDecimal balance;
        private BigDecimal finalBalance;
        private int proximityMetres;
        private List<PlayerStreetView> streets;
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

    // ---- Admin Game View ----
    @Data
    public static class AdminGameView {
        private UUID gameId;
        private String gameName;
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private BigDecimal startingBalance;
        private int proximityMetres;
        private String mapName;
        private List<AdminStreetView> streets;
        private List<LeaderboardEntry> leaderboard;
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
    }
}
