package com.server.realsync.mvc.controllers;

import com.server.realsync.dto.SubscriptionSummaryDto;
import com.server.realsync.entity.Account;
import com.server.realsync.services.AccountService;
import com.server.realsync.services.SubscriptionService;
import com.server.realsync.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/")
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private AccountService accountService;

    @GetMapping("/subscription.html")
    public String getSubscriptionPage(Model model) {
        Account loggedIn = SecurityUtil.getCurrentAccountId();
        Account account = accountService.getById(loggedIn.getId());
        model.addAttribute("account", account);
        return "remindmeui/subscription";
    }

    @ResponseBody
    @GetMapping("/api/account/subscription")
    public SubscriptionSummaryDto getSubscriptionSummary() {
        return subscriptionService.getSubscriptionSummary();
    }

    @ResponseBody
    @PostMapping("/api/subscription/purchase")
    public ResponseEntity<Map<String, Object>> purchaseSubscription(@RequestBody Map<String, Integer> payload) {
        Integer planId = payload.get("planId");
        Integer transactionId = subscriptionService.createPendingTransaction(planId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("transactionId", transactionId);
        return ResponseEntity.ok(response);
    }

    @ResponseBody
    @PostMapping("/api/subscription/mock-success/{transactionId}")
    public ResponseEntity<Map<String, Object>> mockSuccess(@PathVariable Integer transactionId) {
        boolean success = subscriptionService.completePurchase(transactionId);
        Map<String, Object> response = new HashMap<>();
        if (success) {
            response.put("success", true);
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            return ResponseEntity.badRequest().body(response);
        }
    }
}
