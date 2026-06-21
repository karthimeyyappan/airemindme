package com.server.realsync.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import kong.unirest.Unirest;

@Service
public class RealSyncWhatsappService {

    /**
     * Sends the customer_document_ready WhatsApp template.
     * Template variables (6):
     * body_1 = customerName
     * body_2 = "Invoice" (document type)
     * body_3 = invoiceNumber
     * body_4 = publicUrl
     * body_5 = businessName
     * body_6 = businessPhone
     */
    public kong.unirest.HttpResponse<String> sendDocumentReadyTemplate(
            String mobile,
            String customerName,
            String invoiceNumber,
            String publicUrl,
            String businessName,
            String businessPhone) throws Exception {

        String formattedMobile = mobile.trim();
        if (!formattedMobile.startsWith("91") && !formattedMobile.startsWith("+91")) {
            formattedMobile = "91" + formattedMobile;
        }
        if (formattedMobile.startsWith("+")) {
            formattedMobile = formattedMobile.substring(1);
        }

        Map<String, Object> components = new HashMap<>();
        components.put("body_1", Map.of("type", "text", "value", customerName));
        components.put("body_2", Map.of("type", "text", "value", "Invoice"));
        components.put("body_3", Map.of("type", "text", "value", invoiceNumber));
        components.put("body_4", Map.of("type", "text", "value", publicUrl));
        components.put("body_5", Map.of("type", "text", "value", businessName != null ? businessName : ""));
        components.put("body_6", Map.of("type", "text", "value", businessPhone != null ? businessPhone : ""));

        Map<String, Object> toAndComponents = new HashMap<>();
        toAndComponents.put("to", List.of(formattedMobile));
        toAndComponents.put("components", components);

        Map<String, Object> language = new HashMap<>();
        language.put("code", "en");
        language.put("policy", "deterministic");

        Map<String, Object> template = new HashMap<>();
        template.put("name", "customer_document_ready");
        template.put("namespace", "48851ce4_cb2d_4775_b0a6_e7d38323e124");
        template.put("language", language);
        template.put("to_and_components", List.of(toAndComponents));

        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("type", "template");
        payload.put("template", template);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("integrated_number", INTEGRATED_NUMBER);
        requestBody.put("content_type", "template");
        requestBody.put("payload", payload);

        return Unirest.post(API_URL)
                .header("Content-Type", "application/json")
                .header("authkey", AUTH_KEY)
                .body(requestBody)
                .asString();
    }

    private static final String API_URL = "https://api.msg91.com/api/v5/whatsapp/whatsapp-outbound-message/bulk/";
    private static final String AUTH_KEY = "317952AtUwDOh2wv5e43d16cP1";
    private static final String INTEGRATED_NUMBER = "15559290843";

    public kong.unirest.HttpResponse<String> sendReminderTemplate(
            String mobile,
            String customerName,
            String content,
            String businessName,
            String businessMobile) throws Exception {

        // Ensure mobile has country code prefix
        String formattedMobile = mobile.trim();
        if (!formattedMobile.startsWith("91") && !formattedMobile.startsWith("+91")) {
            formattedMobile = "91" + formattedMobile;
        }
        if (formattedMobile.startsWith("+")) {
            formattedMobile = formattedMobile.substring(1);
        }

        // Components (template variables)
        Map<String, Object> components = new HashMap<>();
        components.put("body_1", Map.of(
                "type", "text",
                "value", customerName != null ? customerName : ""));

        components.put("body_2", Map.of(
                "type", "text",
                "value", content != null ? content : ""));

        components.put("body_3", Map.of(
                "type", "text",
                "value", businessName != null ? businessName : ""));

        components.put("body_4", Map.of(
                "type", "text",
                "value", businessMobile != null ? businessMobile : ""));

        // to_and_components
        Map<String, Object> toAndComponents = new HashMap<>();
        toAndComponents.put("to", List.of(formattedMobile));
        toAndComponents.put("components", components);

        // Template config
        Map<String, Object> template = new HashMap<>();
        template.put("name", "numen_reminder");
        template.put("namespace", "48851ce4_cb2d_4775_b0a6_e7d38323e124");

        Map<String, Object> language = new HashMap<>();
        language.put("code", "en");
        language.put("policy", "deterministic");

        template.put("language", language);
        template.put("to_and_components", List.of(toAndComponents));

        // Payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("type", "template");
        payload.put("template", template);

        // Request body wrapper
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("integrated_number", INTEGRATED_NUMBER);
        requestBody.put("content_type", "template");
        requestBody.put("payload", payload);

        // Execute API call
        return Unirest.post(API_URL)
                .header("Content-Type", "application/json")
                .header("authkey", AUTH_KEY)
                .body(requestBody)
                .asString();
    }

    public kong.unirest.HttpResponse<String> sendAppointmentTemplate(
            String mobile,
            String customerName,
            String appointmentConfirmation,
            String appointmentNumber,
            String appointmentUrl,
            String businessName,
            String businessPhone) throws Exception {

        String formattedMobile = mobile.trim();
        if (!formattedMobile.startsWith("91") && !formattedMobile.startsWith("+91")) {
            formattedMobile = "91" + formattedMobile;
        }
        if (formattedMobile.startsWith("+")) {
            formattedMobile = formattedMobile.substring(1);
        }

        Map<String, Object> components = new HashMap<>();
        components.put("body_1", Map.of("type", "text", "value", customerName != null ? customerName : ""));
        components.put("body_2",
                Map.of("type", "text", "value", appointmentConfirmation != null ? appointmentConfirmation : ""));
        components.put("body_3", Map.of("type", "text", "value", appointmentNumber != null ? appointmentNumber : ""));
        components.put("body_4", Map.of("type", "text", "value", appointmentUrl != null ? appointmentUrl : ""));
        components.put("body_5", Map.of("type", "text", "value", businessName != null ? businessName : ""));
        components.put("body_6", Map.of("type", "text", "value", businessPhone != null ? businessPhone : ""));

        Map<String, Object> toAndComponents = new HashMap<>();
        toAndComponents.put("to", List.of(formattedMobile));
        toAndComponents.put("components", components);

        Map<String, Object> language = new HashMap<>();
        language.put("code", "en");
        language.put("policy", "deterministic");

        Map<String, Object> template = new HashMap<>();
        template.put("name", "customer_document_ready");
        template.put("namespace", "48851ce4_cb2d_4775_b0a6_e7d38323e124");
        template.put("language", language);
        template.put("to_and_components", List.of(toAndComponents));

        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("type", "template");
        payload.put("template", template);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("integrated_number", INTEGRATED_NUMBER);
        requestBody.put("content_type", "template");
        requestBody.put("payload", payload);

        return Unirest.post(API_URL)
                .header("Content-Type", "application/json")
                .header("authkey", AUTH_KEY)
                .body(requestBody)
                .asString();
    }

    public kong.unirest.HttpResponse<String> sendReminderImageTemplate(
            String mobile,
            String customerName,
            String content,
            String businessName,
            String businessMobile,
            String imageUrl) throws Exception {

        String formattedMobile = mobile.trim();

        if (!formattedMobile.startsWith("91") && !formattedMobile.startsWith("+91")) {
            formattedMobile = "91" + formattedMobile;
        }

        if (formattedMobile.startsWith("+")) {
            formattedMobile = formattedMobile.substring(1);
        }

        System.out.println("=== IMAGE URL ===");
        System.out.println(imageUrl);

        Map<String, Object> components = new HashMap<>();
        components.put("header_1", Map.of(
                "type", "image",
                "value", imageUrl));
        
                // BODY VARIABLES
                
        components.put("body_1", Map.of(
                "type", "text",
                "value", customerName != null ? customerName : ""));

        components.put("body_2", Map.of(
                "type", "text",
                "value", content != null ? content : ""));

        components.put("body_3", Map.of(
                "type", "text",
                "value", businessName != null ? businessName : ""));

        components.put("body_4", Map.of(
                "type", "text",
                "value", businessMobile != null ? businessMobile : ""));



        Map<String, Object> toAndComponents = new HashMap<>();
        toAndComponents.put("to", List.of(formattedMobile));
        toAndComponents.put("components", components);

        Map<String, Object> language = new HashMap<>();
        language.put("code", "en");
        language.put("policy", "deterministic");

        Map<String, Object> template = new HashMap<>();
        template.put("name", "numen_reminder_image");
        template.put("namespace", "48851ce4_cb2d_4775_b0a6_e7d38323e124");
        template.put("language", language);
        template.put("to_and_components", List.of(toAndComponents));

        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("type", "template");
        payload.put("template", template);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("integrated_number", INTEGRATED_NUMBER);
        requestBody.put("content_type", "template");
        requestBody.put("payload", payload);

        System.out.println("=== MSG91 REQUEST ===");
        System.out.println(requestBody);

        return Unirest.post(API_URL)
                .header("Content-Type", "application/json")
                .header("authkey", AUTH_KEY)
                .body(requestBody)
                .asString();
    }
}
