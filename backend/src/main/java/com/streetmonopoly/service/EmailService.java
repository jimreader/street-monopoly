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

    @Value("${app.api-url}")
    private String apiUrl;

    public void sendInviteEmail(String toEmail, String playerName, String gameName, UUID inviteToken) {
        String inviteLink = apiUrl + "/api/invite/" + inviteToken + "/accept";
        String subject = "You're invited to play Street Monopoly: " + gameName;
        String body = String.format(
            "Hi %s,\n\nYou've been invited to play '%s' on Street Monopoly!\n\n" +
            "Click the link below to accept your invitation:\n%s\n\n" +
            "Get ready to hit the streets and build your empire!\n\nStreet Monopoly",
            playerName, gameName, inviteLink
        );
        sendEmail(toEmail, subject, body);
    }

    public void sendJoinEmail(String toEmail, String playerName, String gameName, UUID joinToken) {
        String joinLink = playerUrl + "/game/" + joinToken;
        String subject = "You've joined Street Monopoly: " + gameName;
        String body = String.format(
            "Hi %s,\n\nYou've accepted your invitation to '%s'!\n\n" +
            "Use this link to access the game when it starts:\n%s\n\n" +
            "Good luck!\n\nStreet Monopoly",
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
