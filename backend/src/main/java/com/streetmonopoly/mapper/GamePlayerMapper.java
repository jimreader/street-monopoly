package com.streetmonopoly.mapper;

import com.streetmonopoly.model.GamePlayer;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Mapper
public interface GamePlayerMapper {

        @Select("SELECT gp.*, p.name as \"player.name\", p.email as \"player.email\" " +
            "FROM game_player gp JOIN player p ON gp.player_id = p.id WHERE gp.game_id = #{gameId} AND gp.deleted_at IS NULL ORDER BY gp.balance DESC")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "gameId", column = "game_id"),
        @Result(property = "eventPlayerId", column = "event_player_id"),
        @Result(property = "playerId", column = "player_id"),
        @Result(property = "balance", column = "balance"),
        @Result(property = "inviteToken", column = "invite_token"),
        @Result(property = "joinToken", column = "join_token"),
        @Result(property = "invitedAt", column = "invited_at"),
        @Result(property = "joinedAt", column = "joined_at"),
        @Result(property = "finalBalance", column = "final_balance"),
        @Result(property = "player.name", column = "player.name"),
        @Result(property = "player.email", column = "player.email")
    })
    List<GamePlayer> findByGameId(UUID gameId);

    @Select("SELECT * FROM game_player WHERE invite_token = #{inviteToken} AND deleted_at IS NULL")
    GamePlayer findByInviteToken(UUID inviteToken);

    @Select("SELECT * FROM game_player WHERE join_token = #{joinToken} AND deleted_at IS NULL")
    GamePlayer findByJoinToken(UUID joinToken);

    @Select("SELECT * FROM game_player WHERE game_id = #{gameId} AND player_id = #{playerId} AND deleted_at IS NULL")
    GamePlayer findByGameAndPlayer(@Param("gameId") UUID gameId, @Param("playerId") UUID playerId);

    @Select("SELECT * FROM game_player WHERE game_id = #{gameId} AND player_id = #{playerId}")
    GamePlayer findByGameAndPlayerIncludingDeleted(@Param("gameId") UUID gameId, @Param("playerId") UUID playerId);

    @Select("SELECT * FROM game_player WHERE event_player_id = #{eventPlayerId} AND deleted_at IS NULL")
    GamePlayer findByEventPlayerId(UUID eventPlayerId);

    @Select("SELECT * FROM game_player WHERE game_id = #{gameId} AND id = #{id} AND deleted_at IS NULL")
    GamePlayer findByGameAndId(@Param("gameId") UUID gameId, @Param("id") UUID id);

        @Insert("INSERT INTO game_player (id, game_id, event_player_id, player_id, balance, invite_token, join_token) " +
            "VALUES (#{id}, #{gameId}, #{eventPlayerId}, #{playerId}, #{balance}, #{inviteToken}, #{joinToken})")
    void insert(GamePlayer gamePlayer);

    @Update("UPDATE game_player SET joined_at = NOW() WHERE id = #{id} AND deleted_at IS NULL")
    void markJoined(UUID id);

    @Update("UPDATE game_player SET balance = #{balance} WHERE id = #{id} AND deleted_at IS NULL")
    void updateBalance(@Param("id") UUID id, @Param("balance") BigDecimal balance);

    @Update("UPDATE game_player SET final_balance = #{finalBalance} WHERE id = #{id} AND deleted_at IS NULL")
    void updateFinalBalance(@Param("id") UUID id, @Param("finalBalance") BigDecimal finalBalance);

    @Update("UPDATE game_player SET device_token = #{deviceToken} WHERE id = #{id} AND deleted_at IS NULL")
    void updateDeviceToken(@Param("id") UUID id, @Param("deviceToken") String deviceToken);

    @Update("UPDATE game_player SET device_token = NULL WHERE id = #{id} AND deleted_at IS NULL")
    void clearDeviceToken(UUID id);

    @Select("SELECT gp.* FROM game_player gp WHERE gp.game_id = #{gameId} AND gp.deleted_at IS NULL")
    List<GamePlayer> findAllByGameId(UUID gameId);

        @Select("SELECT COUNT(*) > 0 FROM game_player gp JOIN player p ON gp.player_id = p.id WHERE gp.game_id = #{gameId} AND gp.deleted_at IS NULL AND LOWER(p.email) = LOWER(#{email})")
        boolean existsActiveByGameAndEmail(@Param("gameId") UUID gameId, @Param("email") String email);

    @Update("UPDATE game_player SET deleted_at = NOW(), device_token = NULL WHERE game_id = #{gameId} AND id = #{id} AND deleted_at IS NULL")
    int softDelete(@Param("gameId") UUID gameId, @Param("id") UUID id);

    @Update("UPDATE game_player SET deleted_at = NOW(), device_token = NULL WHERE game_id = #{gameId} AND deleted_at IS NULL")
    int softDeleteByGameId(UUID gameId);

    @Update("UPDATE game_player SET deleted_at = NULL, joined_at = NOW(), balance = #{balance}, final_balance = NULL, invite_token = #{inviteToken}, join_token = #{joinToken}, device_token = NULL WHERE id = #{id}")
    void restorePlayer(@Param("id") UUID id,
                       @Param("balance") BigDecimal balance,
                       @Param("inviteToken") UUID inviteToken,
                       @Param("joinToken") UUID joinToken);
}
