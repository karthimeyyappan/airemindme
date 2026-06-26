package com.server.realsync.mvc.controllers;

import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.ResponseBody;
import com.server.realsync.repo.UserRepository;
//import com.server.realsync.entity.User;
//import java.util.Map;
//import java.util.Optional;

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

    @GetMapping("/remindmeui/reset-password.html")
    public String resetPassword() {
        return "remindmeui/reset-password";
    }

    @GetMapping("/remindmeui/forgot-password.html")
    public String forgotPassword() {
        return "remindmeui/forgot-password";
    }

}
