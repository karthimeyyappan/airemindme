package com.server.realsync.config;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import com.server.realsync.entity.CustomUserDetails;
import com.server.realsync.entity.User;
import com.server.realsync.repo.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger LOG = LoggerFactory.getLogger(CustomOAuth2SuccessHandler.class);

    @Autowired
    private UserRepository userRepository;

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        if (email == null || email.trim().isEmpty()) {
            LOG.warn("OAuth login failed: email attribute is missing from Google principal");
            response.sendRedirect("/no-account");
            return;
        }

        email = email.trim();
        Optional<User> existingUser = userRepository.findByUsername(email);

        if (!existingUser.isPresent()) {
            LOG.warn("No user record found for Google email: {}", email);
            SecurityContextHolder.clearContext();
            request.getSession().invalidate();
            response.sendRedirect("/no-account");
            return;
        }

        User user = existingUser.get();
        if (user.getAccount() == null) {
            LOG.warn("No account linked for user: {}", email);
            SecurityContextHolder.clearContext();
            request.getSession().invalidate();
            response.sendRedirect("/no-account");
            return;
        }

        // Establish the CustomUserDetails authentication session
        CustomUserDetails customUserDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                customUserDetails,
                null,
                customUserDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(auth);
        securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);

        LOG.info("OAuth login success: {}", email);
        response.sendRedirect("/home.html");
    }
}
