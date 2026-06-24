package com.server.realsync.services;

import com.server.realsync.entity.User;
import com.server.realsync.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PasswordResetService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Send reset link to email
    public void sendResetLink(String email) throws Exception {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new Exception("No account found with this email."));

        // Generate unique token
        String token = UUID.randomUUID().toString();

        // Save token and expiry in user table
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        // Send email
        String resetLink = "http://localhost:8080/reset-password.html?token=" + token;

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom("mindfullmoneyoffical@gmail.com");
        mail.setTo(email);
        mail.setSubject("Numen - Reset Your Password");
        mail.setText(
            "Hi " + user.getFullName() + ",\n\n" +
            "Click the link below to reset your password:\n\n" +
            resetLink + "\n\n" +
            "This link expires in 15 minutes.\n\n" +
            "If you did not request this, please ignore this email.\n\n" +
            "Team Numen"
        );
        mailSender.send(mail);
    }

    // Verify token and update password
    public void resetPassword(String token, String newPassword) throws Exception {
        User user = userRepository.findByResetToken(token)
            .orElseThrow(() -> new Exception("Invalid or expired token."));

        if (user.getResetTokenExpiry() == null ||
            user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new Exception("Reset link has expired. Please request a new one.");
        }

        // Update password and clear token
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }
}