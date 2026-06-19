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

    @GetMapping("/test-appointment")
    public String testAppointment() throws Exception {
        var response = whatsappService.sendAppointmentTemplate(
                "9042529652",
                "Tamil",
                "Confirmed", // appointmentConfirmation
                "APT-12345", // appointmentNumber
                "https://yoururl.com", // appointmentUrl
                "RealSync", // businessName
                "9042529652" // businessPhone
        );

        System.out.println("Status = " + response.getStatus());
        System.out.println("Body = " + response.getBody());

        return response.getBody();
    }

    @GetMapping("/test-reminder-image")
    public String testReminderImage() throws Exception {

        String imageUrl = "https://aipixture.com/img/get/a/88/e/507/m/c/1565/3M3A7837.JPG";
    
        var response = whatsappService.sendReminderImageTemplate(
                "9042529652",
                "Tamil",
                "🎂 Happy Birthday! Wishing you happiness, success and good health.",
                "RealSync Technologies",
                "9042529652",
                imageUrl);

        System.out.println("Status = " + response.getStatus());
        System.out.println("Body = " + response.getBody());

        return response.getBody();
    }

}