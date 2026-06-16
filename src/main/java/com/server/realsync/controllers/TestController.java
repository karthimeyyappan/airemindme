package com.server.realsync.controllers;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.server.realsync.services.RealSyncWhatsappService;

@RestController
public class TestController {

    @Autowired
    private RealSyncWhatsappService whatsappService;

    @GetMapping("/test-whatsapp")
    public String testWhatsapp() throws Exception {

        var response = whatsappService.sendReminderTemplate(
                "9042529652",
                "Tamil",
                "Test Reminder Message",
                "RealSync",
                "9042529652");

        System.out.println("Status = " + response.getStatus());
        System.out.println("Body = " + response.getBody());

        return response.getBody();
    }
}