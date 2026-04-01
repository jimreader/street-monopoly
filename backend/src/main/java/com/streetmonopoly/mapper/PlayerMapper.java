package com.streetmonopoly.mapper;

import com.streetmonopoly.model.Player;
import org.apache.ibatis.annotations.*;

import java.util.UUID;

@Mapper
public interface PlayerMapper {

    @Select("SELECT * FROM player WHERE id = #{id}")
    Player findById(UUID id);

    @Select("SELECT * FROM player WHERE email = #{email}")
    Player findByEmail(String email);

    @Insert("INSERT INTO player (id, name, email) VALUES (#{id}, #{name}, #{email})")
    void insert(Player player);

    @Update("UPDATE player SET name = #{name} WHERE id = #{id}")
    void update(Player player);
}
