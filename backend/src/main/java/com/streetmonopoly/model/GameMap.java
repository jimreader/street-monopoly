package com.streetmonopoly.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class GameMap {
    private UUID id;
    private String name;
    private String postcodeArea;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<Street> streets;
}
