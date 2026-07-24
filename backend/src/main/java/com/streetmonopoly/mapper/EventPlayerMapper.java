package com.streetmonopoly.mapper;

import com.streetmonopoly.model.EventPlayer;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.UUID;

@Mapper
public interface EventPlayerMapper {

    @Select("SELECT ep.*, p.name as \"player.name\", p.email as \"player.email\" " +
            "FROM event_player ep JOIN player p ON ep.player_id = p.id " +
            "WHERE ep.event_id = #{eventId} AND ep.deleted_at IS NULL ORDER BY ep.invited_at ASC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "eventId", column = "event_id"),
            @Result(property = "playerId", column = "player_id"),
            @Result(property = "assignedGameId", column = "assigned_game_id"),
            @Result(property = "inviteToken", column = "invite_token"),
            @Result(property = "joinToken", column = "join_token"),
            @Result(property = "invitedAt", column = "invited_at"),
            @Result(property = "joinedAt", column = "joined_at"),
            @Result(property = "deviceToken", column = "device_token"),
            @Result(property = "player.name", column = "player.name"),
            @Result(property = "player.email", column = "player.email")
    })
    List<EventPlayer> findByEventId(UUID eventId);

    @Select("SELECT * FROM event_player WHERE id = #{id} AND event_id = #{eventId} AND deleted_at IS NULL")
    EventPlayer findByEventAndId(@Param("eventId") UUID eventId, @Param("id") UUID id);

    @Select("SELECT * FROM event_player WHERE event_id = #{eventId} AND player_id = #{playerId} AND deleted_at IS NULL")
    EventPlayer findByEventAndPlayer(@Param("eventId") UUID eventId, @Param("playerId") UUID playerId);

    @Select("SELECT * FROM event_player WHERE event_id = #{eventId} AND player_id = #{playerId}")
    EventPlayer findByEventAndPlayerIncludingDeleted(@Param("eventId") UUID eventId, @Param("playerId") UUID playerId);

    @Select("SELECT ep.* FROM event_player ep JOIN player p ON ep.player_id = p.id WHERE ep.event_id = #{eventId} AND ep.deleted_at IS NULL AND LOWER(p.email) = LOWER(#{email}) LIMIT 1")
    EventPlayer findActiveByEventAndEmail(@Param("eventId") UUID eventId, @Param("email") String email);

    @Select("SELECT * FROM event_player WHERE join_token = #{joinToken} AND deleted_at IS NULL")
    EventPlayer findByJoinToken(UUID joinToken);

    @Insert("INSERT INTO event_player (id, event_id, player_id, invite_token, join_token) VALUES (#{id}, #{eventId}, #{playerId}, #{inviteToken}, #{joinToken})")
    void insert(EventPlayer eventPlayer);

    @Update("UPDATE event_player SET joined_at = NOW() WHERE id = #{id} AND deleted_at IS NULL")
    void markJoined(UUID id);

    @Update("UPDATE event_player SET assigned_game_id = #{assignedGameId} WHERE id = #{id} AND deleted_at IS NULL")
    void assignToGame(@Param("id") UUID id, @Param("assignedGameId") UUID assignedGameId);

    @Update("UPDATE event_player SET device_token = #{deviceToken} WHERE id = #{id} AND deleted_at IS NULL")
    void updateDeviceToken(@Param("id") UUID id, @Param("deviceToken") String deviceToken);

    @Update("UPDATE event_player SET device_token = NULL WHERE id = #{id} AND deleted_at IS NULL")
    void clearDeviceToken(UUID id);

    @Update("UPDATE event_player SET deleted_at = NULL, joined_at = NOW(), invite_token = #{inviteToken}, join_token = #{joinToken}, assigned_game_id = NULL, device_token = NULL WHERE id = #{id}")
    void restorePlayer(@Param("id") UUID id, @Param("inviteToken") UUID inviteToken, @Param("joinToken") UUID joinToken);

    @Update("UPDATE event_player SET deleted_at = NOW(), assigned_game_id = NULL, device_token = NULL WHERE event_id = #{eventId} AND id = #{id} AND deleted_at IS NULL")
    int softDelete(@Param("eventId") UUID eventId, @Param("id") UUID id);

    @Update("UPDATE event_player SET deleted_at = NOW(), assigned_game_id = NULL, device_token = NULL WHERE event_id = #{eventId} AND deleted_at IS NULL")
    int softDeleteByEventId(UUID eventId);
}
