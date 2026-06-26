package com.server.realsync.controllers;

import com.server.realsync.entity.User;
import com.server.realsync.repo.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Optional;

import com.server.realsync.services.PasswordResetService;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@RestController
public class OAuth2Controller {
    private final OAuth2AuthorizedClientService clientService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    public OAuth2Controller(OAuth2AuthorizedClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping("/login/oauth2/code/{provider}")
    public RedirectView loginSuccess(
            @PathVariable String provider,
            OAuth2AuthenticationToken authenticationToken) {

        System.out.println("=== OAUTH CALLBACK ===");

        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(
                authenticationToken.getAuthorizedClientRegistrationId(),
                authenticationToken.getName());

        String userEmail = client.getPrincipalName();

        System.out.println("EMAIL = " + userEmail);

        Optional<User> existingUser = userRepository.findByUsername(userEmail);

        System.out.println("USER FOUND = " + existingUser.isPresent());

        return new RedirectView("/login-success");
    }

    @PostMapping("/api/auth/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        try {
            passwordResetService.sendResetLink(body.get("email"));
            return ResponseEntity.ok(Map.of("message", "Reset link sent to your email."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/auth/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        try {
            passwordResetService.resetPassword(body.get("token"), body.get("password"));
            return ResponseEntity.ok(Map.of("message", "Password reset successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
