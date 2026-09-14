package com.streetmonopoly.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Runs after the inviting transaction commits so email delivery never blocks or races the invite request.
@Component
public class PlayerInvitedEventListener {

    @Autowired private RetryableEmailSender retryableEmailSender;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPlayerInvited(PlayerInvitedEvent event) {
        retryableEmailSender.sendJoinEmailWithRetry(event);
    }
}
