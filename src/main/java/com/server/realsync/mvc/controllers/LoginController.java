package com.server.realsync.mvc.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.server.realsync.repo.UserRepository;
import com.server.realsync.entity.User;
import java.util.Map;
import java.util.Optional;

@Controller
public class LoginController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/login1")
    public String login() {
        return "auth/login"; // Return the view name for the login page
    }

    @GetMapping("/register")
    public String register(@RequestParam(name = "refAccId", required = false) String refAccId, Model model) {
    	model.addAttribute("refAccId", refAccId);
        return "auth/register"; // Return the view name for the login page
    }

    @PostMapping("/api/auth/forgot-password")
    @ResponseBody
    public ResponseEntity<String> forgotPassword(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Email is required");
        }

        Optional<User> userOpt = userRepository.findByUsername(email.trim());
        if (!userOpt.isPresent()) {
            return ResponseEntity.badRequest().body("Account not found.");
        }

        return ResponseEntity.ok("Password reset instructions have been sent to your email.");
    }

}
