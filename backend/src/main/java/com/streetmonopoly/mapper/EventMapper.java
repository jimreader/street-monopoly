package com.streetmonopoly.mapper;

import com.streetmonopoly.model.Event;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.UUID;

@Mapper
public interface EventMapper {

    @Select("SELECT * FROM event WHERE deleted_at IS NULL ORDER BY created_at DESC")
    List<Event> findAll();

    @Select("SELECT * FROM event WHERE id = #{id} AND deleted_at IS NULL")
    Event findById(UUID id);

        @Insert("INSERT INTO event (id, name, logo_image_url, game_map_id, start_time, end_time, starting_balance, proximity_metres, max_players_per_game, status) " +
            "VALUES (#{id}, #{name}, #{logoImageUrl}, #{gameMapId}, #{startTime}, #{endTime}, #{startingBalance}, #{proximityMetres}, #{maxPlayersPerGame}, #{status})")
    void insert(Event event);

        @Update("UPDATE event SET name = #{name}, logo_image_url = #{logoImageUrl}, game_map_id = #{gameMapId}, start_time = #{startTime}, end_time = #{endTime}, " +
            "starting_balance = #{startingBalance}, proximity_metres = #{proximityMetres}, max_players_per_game = #{maxPlayersPerGame} " +
            "WHERE id = #{id} AND deleted_at IS NULL")
        int updateDetails(Event event);

    @Update("UPDATE event SET status = #{status} WHERE id = #{id} AND deleted_at IS NULL")
    void updateStatus(@Param("id") UUID id, @Param("status") String status);

    @Update("UPDATE event SET deleted_at = NOW() WHERE id = #{id} AND deleted_at IS NULL")
    int softDelete(UUID id);

    @Select("SELECT * FROM event WHERE deleted_at IS NULL AND status = 'pending' AND start_time <= NOW()")
    List<Event> findPendingReadyToStart();

    @Select("SELECT * FROM event WHERE deleted_at IS NULL AND status = 'active' AND end_time <= NOW()")
    List<Event> findActiveReadyToEnd();
}
