package com.streetmonopoly.controller;

import com.streetmonopoly.dto.Dtos.*;
import com.streetmonopoly.model.GameMap;
import com.streetmonopoly.model.Street;
import com.streetmonopoly.service.GameMapService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/maps")
public class GameMapController {

    @Autowired
    private GameMapService gameMapService;

    @GetMapping
    public List<GameMap> getAllMaps() {
        return gameMapService.getAllMaps();
    }

    @GetMapping("/{id}")
    public GameMap getMap(@PathVariable UUID id) {
        return gameMapService.getMap(id);
    }

    @PostMapping
    public GameMap createMap(@Valid @RequestBody CreateGameMapRequest request) {
        return gameMapService.createMap(request);
    }

    @PutMapping("/{id}")
    public GameMap updateMap(@PathVariable UUID id, @Valid @RequestBody CreateGameMapRequest request) {
        return gameMapService.updateMap(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMap(@PathVariable UUID id) {
        gameMapService.deleteMap(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{mapId}/streets")
    public Street addStreet(@PathVariable UUID mapId, @Valid @RequestBody CreateStreetRequest request) {
        return gameMapService.addStreet(mapId, request);
    }

    @PutMapping("/streets/{streetId}")
    public Street updateStreet(@PathVariable UUID streetId, @Valid @RequestBody CreateStreetRequest request) {
        return gameMapService.updateStreet(streetId, request);
    }

    @DeleteMapping("/streets/{streetId}")
    public ResponseEntity<Void> deleteStreet(@PathVariable UUID streetId) {
        gameMapService.deleteStreet(streetId);
        return ResponseEntity.noContent().build();
    }
}
