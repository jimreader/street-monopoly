package com.streetmonopoly.service;

import com.streetmonopoly.mapper.EventPlayerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RetryableEmailSender {

    @Autowired private EmailService emailService;
    @Autowired private EventPlayerMapper eventPlayerMapper;

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void sendJoinEmailWithRetry(PlayerInvitedEvent event) {
        emailService.sendJoinEmail(event.email(), event.playerName(), event.eventName(), event.joinToken());
        eventPlayerMapper.updateInviteEmailStatus(event.eventPlayerId(), "sent", LocalDateTime.now(), null);
    }

    @Recover
    public void recover(Exception e, PlayerInvitedEvent event) {
        System.err.println("Giving up sending invite email to " + event.email() + " after retries: " + e.getMessage());
        eventPlayerMapper.updateInviteEmailStatus(event.eventPlayerId(), "failed", null, truncate(e.getMessage()));
    }

    private String truncate(String message) {
        if (message == null) return "Unknown error";
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
