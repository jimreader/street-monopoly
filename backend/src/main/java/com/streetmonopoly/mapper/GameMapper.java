package com.streetmonopoly.mapper;

import com.streetmonopoly.model.Game;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.UUID;

@Mapper
public interface GameMapper {

    @Select("SELECT * FROM game WHERE deleted_at IS NULL ORDER BY created_at DESC")
    List<Game> findAll();

    @Select("SELECT * FROM game WHERE id = #{id} AND deleted_at IS NULL")
    Game findById(UUID id);

    @Select("SELECT * FROM game WHERE event_id = #{eventId} AND deleted_at IS NULL ORDER BY created_at ASC")
    List<Game> findByEventId(UUID eventId);

    @Select("SELECT * FROM game WHERE deleted_at IS NULL AND status IN ('active', 'completed') ORDER BY start_time DESC")
    List<Game> findActiveAndCompleted();

    @Select("SELECT * FROM game WHERE deleted_at IS NULL AND status = 'pending' AND start_time <= NOW()")
    List<Game> findPendingReadyToStart();

    @Select("SELECT * FROM game WHERE deleted_at IS NULL AND status = 'active' AND end_time <= NOW()")
    List<Game> findActiveReadyToEnd();

        @Insert("INSERT INTO game (id, event_id, name, game_map_id, start_time, end_time, starting_balance, proximity_metres, unvisited_street_penalty_percent, status) " +
            "VALUES (#{id}, #{eventId}, #{name}, #{gameMapId}, #{startTime}, #{endTime}, #{startingBalance}, #{proximityMetres}, #{unvisitedStreetPenaltyPercent}, #{status})")
    void insert(Game game);

    @Update("UPDATE game SET status = #{status} WHERE id = #{id} AND deleted_at IS NULL")
    void updateStatus(@Param("id") UUID id, @Param("status") String status);

    @Update("UPDATE game SET deleted_at = NOW() WHERE id = #{id} AND deleted_at IS NULL")
    int softDelete(UUID id);
}
