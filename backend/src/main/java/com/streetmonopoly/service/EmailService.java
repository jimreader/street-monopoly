package com.streetmonopoly.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.player-url}")
    private String playerUrl;

    public void sendJoinEmail(String toEmail, String playerName, String gameName, UUID joinToken) {
        String joinLink = playerUrl + "/game/" + joinToken;
        String subject = "You're in! Street Monopoly: " + gameName;
        String body = String.format(
            "Hi %s,\n\n" +
            "You've been added to '%s' on Street Monopoly!\n\n" +
            "Tap the link below to join the game:\n%s\n\n" +
            "Save this link — you'll need it to play when the game starts.\n\n" +
            "Good luck!\nStreet Monopoly",
            playerName, gameName, joinLink
        );
        sendEmail(toEmail, subject, body);
    }

    private void sendEmail(String to, String subject, String body) {
        if (mailSender == null) {
            System.out.println("=== EMAIL (mail sender not configured) ===");
            System.out.println("To: " + to);
            System.out.println("Subject: " + subject);
            System.out.println("Body: " + body);
            System.out.println("==========================================");
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send email to " + to + ": " + e.getMessage());
            // Log but don't fail the operation
        }
    }
}
