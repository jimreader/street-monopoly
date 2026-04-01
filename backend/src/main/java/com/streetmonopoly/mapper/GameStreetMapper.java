package com.streetmonopoly.mapper;

import com.streetmonopoly.model.GameStreet;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.UUID;

@Mapper
public interface GameStreetMapper {

    @Select("SELECT gs.*, s.name as street_name, s.price as street_price, s.rental_price as street_rental_price, " +
            "s.colour as street_colour, s.latitude as street_latitude, s.longitude as street_longitude, " +
            "s.image_clue_url as street_image_clue_url, p.name as owner_name " +
            "FROM game_street gs " +
            "JOIN street s ON gs.street_id = s.id " +
            "LEFT JOIN player p ON gs.owner_player_id = p.id " +
            "WHERE gs.game_id = #{gameId} ORDER BY s.colour, s.name")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "gameId", column = "game_id"),
        @Result(property = "streetId", column = "street_id"),
        @Result(property = "ownerPlayerId", column = "owner_player_id"),
        @Result(property = "purchasedAt", column = "purchased_at"),
        @Result(property = "street.name", column = "street_name"),
        @Result(property = "street.price", column = "street_price"),
        @Result(property = "street.rentalPrice", column = "street_rental_price"),
        @Result(property = "street.colour", column = "street_colour"),
        @Result(property = "street.latitude", column = "street_latitude"),
        @Result(property = "street.longitude", column = "street_longitude"),
        @Result(property = "street.imageClueUrl", column = "street_image_clue_url"),
        @Result(property = "ownerPlayer.name", column = "owner_name")
    })
    List<GameStreet> findByGameId(UUID gameId);

    @Select("SELECT * FROM game_street WHERE game_id = #{gameId} AND street_id = #{streetId}")
    GameStreet findByGameAndStreet(@Param("gameId") UUID gameId, @Param("streetId") UUID streetId);

    @Insert("INSERT INTO game_street (id, game_id, street_id) VALUES (#{id}, #{gameId}, #{streetId})")
    void insert(GameStreet gameStreet);

    @Update("UPDATE game_street SET owner_player_id = #{ownerPlayerId}, purchased_at = NOW() WHERE id = #{id}")
    void updateOwner(@Param("id") UUID id, @Param("ownerPlayerId") UUID ownerPlayerId);

    @Select("SELECT COUNT(*) FROM game_street WHERE game_id = #{gameId} AND owner_player_id = #{playerId}")
    int countOwnedByPlayer(@Param("gameId") UUID gameId, @Param("playerId") UUID playerId);
}
