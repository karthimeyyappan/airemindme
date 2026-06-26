package com.server.realsync.controllers;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.server.realsync.dto.PasswordResetDto;
import com.server.realsync.entity.Account;
import com.server.realsync.entity.CustomUserDetails;
import com.server.realsync.entity.User;
import com.server.realsync.services.AccountService;
import com.server.realsync.services.UserService;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.ResponseBody;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    private AccountService service;

    @Autowired
    private UserService userService;

    @Autowired
    private com.server.realsync.repo.AccountPlanRepository accountPlanRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public AccountController() {
    }

    @PostMapping("/signup")
    public ResponseEntity<com.server.realsync.dto.SignupResponseDto> signup(
            @RequestBody com.server.realsync.dto.SignupRequestDto requestDto) {
        try {
            service.registerAccount(requestDto);
            return ResponseEntity
                    .ok(new com.server.realsync.dto.SignupResponseDto(true, "Account created successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new com.server.realsync.dto.SignupResponseDto(false, e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(new com.server.realsync.dto.SignupResponseDto(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new com.server.realsync.dto.SignupResponseDto(false,
                            "Registration failed: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Account>> getAllAccounts() {
        List<Account> accounts = service.findAll();
        return new ResponseEntity<>(accounts, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccountById(@PathVariable Integer id) {
        Optional<Account> account = service.findById(id);
        return account.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Account> createAccount(@RequestBody Account account) {
        Account savedAccount = service.save(account);
        return new ResponseEntity<>(savedAccount, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Account> updateAccount(@PathVariable Integer id, @RequestBody Account account) {
        Account existingAccount = service.findById(id).get();
        if (existingAccount != null) {
            existingAccount.setName(account.getName());
            existingAccount.setMobile(account.getMobile());
            existingAccount.setEmail(account.getEmail());
            existingAccount.setAddress(account.getAddress());
            Account updatedAccount = service.save(existingAccount);
            return new ResponseEntity<>(updatedAccount, HttpStatus.OK);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Integer id) {
        if (service.findById(id).isPresent()) {
            service.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/resetAppPassword")
    public ResponseEntity<User> resetPassword(@RequestBody PasswordResetDto passwordResetDto) {
        User user = userService.findByUserId(passwordResetDto.getUserId());
        if (passwordEncoder.matches(passwordResetDto.getCurrentPassword(), user.getPassword())) {
            //
            user.setPassword(new BCryptPasswordEncoder().encode(passwordResetDto.getNewPassword()));
            userService.saveUser(user);
            return new ResponseEntity<User>(user, HttpStatus.OK);
        } else {
            return new ResponseEntity<User>(user, HttpStatus.INSUFFICIENT_STORAGE);
        }

    }

    @PostMapping("/saveUser")
    public ResponseEntity<User> saveUser(@RequestBody User user) {
        User existingUser = null;
        try {
            existingUser = userService.findByUsername(user.getUsername());
        } catch (Exception e) {
            System.out.println("User not found:" + user.getUsername());
        }
        if (existingUser == null) {
            // create
            user.setPassword(new BCryptPasswordEncoder().encode(user.getPassword()));
            int accountId = 0;
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
                CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
                accountId = customUserDetails.getAccountId();
            }

            Account account = new Account();
            account.setId(accountId);
            user.setAccount(account);
            userService.saveUser(user);
            return new ResponseEntity<User>(user, HttpStatus.OK);
        } else {
            // update
            existingUser.setPassword(new BCryptPasswordEncoder().encode(user.getPassword()));
            existingUser.setFullName(user.getFullName());
            existingUser.setRole(user.getRole());
            existingUser.setEmail(user.getEmail());
            userService.saveUser(existingUser);
            return new ResponseEntity<User>(user, HttpStatus.OK);
        }
    }

    @GetMapping("/check-email")
    public ResponseEntity<Boolean> checkEmail(String email) {
        return ResponseEntity.ok(service.emailExists(email));
    }

    @GetMapping("/check-mobile")
    public ResponseEntity<Boolean> checkMobile(String mobile) {
        return ResponseEntity.ok(service.mobileExists(mobile));
    }

    @GetMapping("/check-referral")
    @ResponseBody
    public ResponseEntity<?> checkReferral(@RequestParam String code) {
        String cleaned = code.trim().toUpperCase();
        if (!cleaned.startsWith("NUMEN-")) {
            return ResponseEntity.ok(Map.of("valid", false, "message", "Invalid referral code format"));
        }
        try {
            Integer referrerId = Integer.parseInt(cleaned.replace("NUMEN-", "").trim());
            Optional<Account> account = service.findById(referrerId);
            if (account.isPresent()) {
                return ResponseEntity.ok(Map.of(
                        "valid", true,
                        "message", "Referral code accepted — " + account.get().getName()));
            } else {
                return ResponseEntity.ok(Map.of("valid", false, "message", "Referral code not found"));
            }
        } catch (NumberFormatException e) {
            return ResponseEntity.ok(Map.of("valid", false, "message", "Invalid referral code"));
        }
    }

    @GetMapping("/credit-status")
    @ResponseBody
    public ResponseEntity<?> getCreditStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        int accountId = userDetails.getAccountId();
        
        Optional<com.server.realsync.entity.AccountPlan> planOpt = accountPlanRepository
            .findByAccountIdAndStatus(accountId, com.server.realsync.entity.AccountPlan.PlanStatus.active);
        
        if (planOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "hasActivePlan", false,
                "balance", 0,
                "planExpired", true
            ));
        }
        
        com.server.realsync.entity.AccountPlan plan = planOpt.get();
        boolean expired = plan.getEndDate() != null && 
                          plan.getEndDate().isBefore(java.time.LocalDate.now());
        
        return ResponseEntity.ok(Map.of(
            "hasActivePlan", true,
            "balance", plan.getBalance(),
            "planExpired", expired,
            "planName", plan.getPlan().getName()
        ));
    }

    @GetMapping("/wallet-balance")
    @ResponseBody
    public ResponseEntity<?> getWalletBalance() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        int accountId = userDetails.getAccountId();
        Account fullAccount = service.findById(accountId).orElse(null);
        if (fullAccount == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Account not found"));
        }
        return ResponseEntity.ok(Map.of(
            "walletBalance", fullAccount.getWalletBalance() != null ? fullAccount.getWalletBalance() : 0.0
        ));
    }
}

@RestController
class CurrentAccountApiController {

    @Autowired
    private AccountService accountService;

    @GetMapping("/api/account/current")
    @ResponseBody
    public ResponseEntity<?> getCurrentAccount() {
        try {
            Authentication authentication = SecurityContextHolder
                    .getContext().getAuthentication();

            if (authentication == null ||
                    !(authentication.getPrincipal() instanceof CustomUserDetails)) {
                return ResponseEntity.status(401)
                        .body(Map.of("error", "Not logged in"));
            }

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            int accountId = userDetails.getAccountId();

            Account account = accountService.findById(accountId).orElse(null);

            if (account == null) {
                return ResponseEntity.status(404)
                        .body(Map.of("error", "Account not found"));
            }

            Map<String, Object> result = new HashMap<>();
            result.put("id", account.getId());
            result.put("name", account.getName());
            result.put("email", account.getEmail());
            result.put("businessName", account.getBusinessName());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}