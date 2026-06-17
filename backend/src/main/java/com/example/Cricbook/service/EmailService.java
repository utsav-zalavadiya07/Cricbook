package com.example.Cricbook.service;

import com.example.Cricbook.exception.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendOtp(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("OTP Verification for Cricbook");
        message.setText("Your OTP is: " + otp + ". This is valid for 5 minutes.");
        try {
            mailSender.send(message);
        } catch (Exception e) {
            // Log error but we don't throw to prevent complete failure during dev if SMTP is not configured
            System.err.println("Failed to send email to " + to + ": " + e.getMessage());
        }
    }

    public void sendPasswordResetLink(String to, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Reset your Cricbook password");
        message.setText("""
                We received a request to reset your Cricbook password.

                Reset password:
                %s

                This link is valid for 15 minutes and can be used only once.
                If you did not request this, you can safely ignore this email.
                """.formatted(resetLink));
        try {
            mailSender.send(message);
        } catch (Exception e) {
            // Keep auth flows usable in local/dev when SMTP is not configured.
            System.err.println("Failed to send password reset email to " + to + ": " + e.getMessage());
            System.err.println("Password reset link for local testing: " + resetLink);
        }
    }

    public void send(String to, String title, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(title);
        message.setText(content);
        try {
            mailSender.send(message);
        } catch (org.springframework.mail.MailAuthenticationException e) {
            System.err.println("Mail authentication failed for " + to + ": " + e.getMessage());
            throw ApiException.serviceUnavailable("Email service is temporarily unavailable.");
        } catch (org.springframework.mail.MailSendException e) {
            System.err.println("Mail send failed for " + to + ": " + e.getMessage());
            throw ApiException.serviceUnavailable("Unable to send email right now. Please try again later.");
        } catch (Exception e) {
            System.err.println("Failed to send email to " + to + ": " + e.getMessage());
            throw ApiException.internal("Unexpected error while sending email.", e);
        }
    }
}
