package com.streetmonopoly.mapper;

import com.streetmonopoly.model.GameMap;
import com.streetmonopoly.model.Street;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.UUID;

@Mapper
public interface GameMapMapper {

    @Select("SELECT * FROM game_map ORDER BY created_at DESC")
    List<GameMap> findAll();

    @Select("SELECT * FROM game_map WHERE id = #{id}")
    GameMap findById(UUID id);

    @Insert("INSERT INTO game_map (id, name, postcode_area) VALUES (#{id}, #{name}, #{postcodeArea})")
    void insert(GameMap gameMap);

    @Update("UPDATE game_map SET name = #{name}, postcode_area = #{postcodeArea}, updated_at = NOW() WHERE id = #{id}")
    void update(GameMap gameMap);

    @Delete("DELETE FROM game_map WHERE id = #{id}")
    void delete(UUID id);

    @Select("SELECT * FROM street WHERE game_map_id = #{gameMapId} ORDER BY colour, name")
    List<Street> findStreetsByMapId(UUID gameMapId);

    @Select("SELECT * FROM street WHERE id = #{id}")
    Street findStreetById(UUID id);

    @Insert("INSERT INTO street (id, game_map_id, name, price, rental_price, colour, latitude, longitude, image_clue_url) " +
            "VALUES (#{id}, #{gameMapId}, #{name}, #{price}, #{rentalPrice}, #{colour}, #{latitude}, #{longitude}, #{imageClueUrl})")
    void insertStreet(Street street);

    @Update("UPDATE street SET name=#{name}, price=#{price}, rental_price=#{rentalPrice}, colour=#{colour}, " +
            "latitude=#{latitude}, longitude=#{longitude}, image_clue_url=#{imageClueUrl} WHERE id = #{id}")
    void updateStreet(Street street);

    @Delete("DELETE FROM street WHERE id = #{id}")
    void deleteStreet(UUID id);
}
