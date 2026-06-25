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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

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
}
