package com.streetmonopoly.mapper;

import com.streetmonopoly.model.GamePlayer;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Mapper
public interface GamePlayerMapper {

    @Select("SELECT gp.*, p.name as \"player.name\", p.email as \"player.email\" " +
            "FROM game_player gp JOIN player p ON gp.player_id = p.id WHERE gp.game_id = #{gameId} ORDER BY gp.balance DESC")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "gameId", column = "game_id"),
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

    @Select("SELECT * FROM game_player WHERE invite_token = #{inviteToken}")
    GamePlayer findByInviteToken(UUID inviteToken);

    @Select("SELECT * FROM game_player WHERE join_token = #{joinToken}")
    GamePlayer findByJoinToken(UUID joinToken);

    @Select("SELECT * FROM game_player WHERE game_id = #{gameId} AND player_id = #{playerId}")
    GamePlayer findByGameAndPlayer(@Param("gameId") UUID gameId, @Param("playerId") UUID playerId);

    @Insert("INSERT INTO game_player (id, game_id, player_id, balance, invite_token, join_token) " +
            "VALUES (#{id}, #{gameId}, #{playerId}, #{balance}, #{inviteToken}, #{joinToken})")
    void insert(GamePlayer gamePlayer);

    @Update("UPDATE game_player SET joined_at = NOW() WHERE id = #{id}")
    void markJoined(UUID id);

    @Update("UPDATE game_player SET balance = #{balance} WHERE id = #{id}")
    void updateBalance(@Param("id") UUID id, @Param("balance") BigDecimal balance);

    @Update("UPDATE game_player SET final_balance = #{finalBalance} WHERE id = #{id}")
    void updateFinalBalance(@Param("id") UUID id, @Param("finalBalance") BigDecimal finalBalance);

    @Update("UPDATE game_player SET device_token = #{deviceToken} WHERE id = #{id}")
    void updateDeviceToken(@Param("id") UUID id, @Param("deviceToken") String deviceToken);

    @Select("SELECT gp.* FROM game_player gp WHERE gp.game_id = #{gameId}")
    List<GamePlayer> findAllByGameId(UUID gameId);
}
