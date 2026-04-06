package com.streetmonopoly.mapper;

import com.streetmonopoly.model.StreetVisit;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.UUID;

@Mapper
public interface StreetVisitMapper {

    @Select("SELECT * FROM street_visit WHERE game_id = #{gameId} AND player_id = #{playerId}")
    List<StreetVisit> findByGameAndPlayer(@Param("gameId") UUID gameId, @Param("playerId") UUID playerId);

    @Select("SELECT * FROM street_visit WHERE game_id = #{gameId} AND street_id = #{streetId} AND player_id = #{playerId}")
    StreetVisit findByGameStreetAndPlayer(@Param("gameId") UUID gameId, @Param("streetId") UUID streetId, @Param("playerId") UUID playerId);

    @Insert("INSERT INTO street_visit (id, game_id, street_id, player_id, visit_type, amount) " +
            "VALUES (#{id}, #{gameId}, #{streetId}, #{playerId}, #{visitType}, #{amount})")
    void insert(StreetVisit visit);

    @Select("SELECT * FROM street_visit WHERE game_id = #{gameId}")
    List<StreetVisit> findByGameId(UUID gameId);

    @Select("SELECT * FROM street_visit WHERE game_id = #{gameId} AND street_id = #{streetId}")
    List<StreetVisit> findByGameAndStreet(@Param("gameId") UUID gameId, @Param("streetId") UUID streetId);

    @Select("SELECT DISTINCT ON (player_id) * FROM street_visit " +
            "WHERE game_id = #{gameId} ORDER BY player_id, visited_at DESC")
    List<StreetVisit> findLatestVisitPerPlayer(UUID gameId);

    @Select("SELECT sv.* FROM street_visit sv " +
            "JOIN game_street gs ON sv.game_id = gs.game_id AND sv.street_id = gs.street_id " +
            "WHERE sv.game_id = #{gameId} AND gs.owner_player_id = #{ownerPlayerId} " +
            "AND sv.visit_type = 'rent_paid' ORDER BY sv.visited_at DESC")
    List<StreetVisit> findRentPaidToOwner(@Param("gameId") UUID gameId, @Param("ownerPlayerId") UUID ownerPlayerId);
}
