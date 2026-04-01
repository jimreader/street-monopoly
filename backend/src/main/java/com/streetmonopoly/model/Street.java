package com.streetmonopoly.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class Street {
    private UUID id;
    private UUID gameMapId;
    private String name;
    private BigDecimal price;
    private BigDecimal rentalPrice;
    private String colour;
    private double latitude;
    private double longitude;
    private String imageClueUrl;
    private LocalDateTime createdAt;
}
