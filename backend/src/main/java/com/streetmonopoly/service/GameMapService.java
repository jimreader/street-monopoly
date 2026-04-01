package com.streetmonopoly.service;

import com.streetmonopoly.dto.Dtos.*;
import com.streetmonopoly.mapper.GameMapMapper;
import com.streetmonopoly.model.GameMap;
import com.streetmonopoly.model.Street;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class GameMapService {

    @Autowired
    private GameMapMapper gameMapMapper;

    public List<GameMap> getAllMaps() {
        List<GameMap> maps = gameMapMapper.findAll();
        for (GameMap map : maps) {
            map.setStreets(gameMapMapper.findStreetsByMapId(map.getId()));
        }
        return maps;
    }

    public GameMap getMap(UUID id) {
        GameMap map = gameMapMapper.findById(id);
        if (map == null) throw new RuntimeException("Game map not found: " + id);
        map.setStreets(gameMapMapper.findStreetsByMapId(id));
        return map;
    }

    public GameMap createMap(CreateGameMapRequest request) {
        GameMap map = new GameMap();
        map.setId(UUID.randomUUID());
        map.setName(request.getName());
        map.setPostcodeArea(request.getPostcodeArea());
        gameMapMapper.insert(map);
        return getMap(map.getId());
    }

    public GameMap updateMap(UUID id, CreateGameMapRequest request) {
        GameMap map = getMap(id);
        map.setName(request.getName());
        map.setPostcodeArea(request.getPostcodeArea());
        gameMapMapper.update(map);
        return getMap(id);
    }

    public void deleteMap(UUID id) {
        gameMapMapper.delete(id);
    }

    public Street addStreet(UUID mapId, CreateStreetRequest request) {
        getMap(mapId); // verify exists
        Street street = new Street();
        street.setId(UUID.randomUUID());
        street.setGameMapId(mapId);
        street.setName(request.getName());
        street.setPrice(request.getPrice());
        street.setRentalPrice(request.getRentalPrice());
        street.setColour(request.getColour());
        street.setLatitude(request.getLatitude());
        street.setLongitude(request.getLongitude());
        street.setImageClueUrl(request.getImageClueUrl());
        gameMapMapper.insertStreet(street);
        return gameMapMapper.findStreetById(street.getId());
    }

    public Street updateStreet(UUID streetId, CreateStreetRequest request) {
        Street street = gameMapMapper.findStreetById(streetId);
        if (street == null) throw new RuntimeException("Street not found: " + streetId);
        street.setName(request.getName());
        street.setPrice(request.getPrice());
        street.setRentalPrice(request.getRentalPrice());
        street.setColour(request.getColour());
        street.setLatitude(request.getLatitude());
        street.setLongitude(request.getLongitude());
        street.setImageClueUrl(request.getImageClueUrl());
        gameMapMapper.updateStreet(street);
        return gameMapMapper.findStreetById(streetId);
    }

    public void deleteStreet(UUID streetId) {
        gameMapMapper.deleteStreet(streetId);
    }
}
