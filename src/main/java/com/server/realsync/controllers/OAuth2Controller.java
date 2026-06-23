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

@RestController
public class OAuth2Controller {
    private final OAuth2AuthorizedClientService clientService;

    @Autowired
    private UserRepository userRepository;

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
}
