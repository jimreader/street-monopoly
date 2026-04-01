package com.streetmonopoly.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class Player {
    private UUID id;
    private String name;
    private String email;
    private LocalDateTime createdAt;
}
