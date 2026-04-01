package com.streetmonopoly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StreetMonopolyApplication {
    public static void main(String[] args) {
        SpringApplication.run(StreetMonopolyApplication.class, args);
    }
}
