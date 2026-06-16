package com.server.realsync.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

/**
 * 
 */
import kong.unirest.Unirest;

@Service
public class WhatsAppSender {

    public static void main(String[] args) {

        try {

            WhatsAppSender whatsAppSender = new WhatsAppSender();
            whatsAppSender.sendEventPhotosLink(List.of("919042529652"), "Muthu1", "Wedding Event1",
                    "https://aipixture.com/event/1/order", "Muthu Studio", "+91-9789562069");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendEventPhotosLink(List<String> recipients, String body1, String body2, String body3,
            String body4, String body5) {

        try {

            // 🔹 Components (template variables)
            Map<String, Object> components = new HashMap<>();
            components.put("body_1", Map.of("type", "text", "value", body1));
            components.put("body_2", Map.of("type", "text", "value", body2));
            components.put("body_3", Map.of("type", "text", "value", body3));
            components.put("body_4", Map.of("type", "text", "value", body4));
            components.put("body_5", Map.of("type", "text", "value", body5));

            // 🔹 to_and_components
            Map<String, Object> toAndComponents = new HashMap<>();
            toAndComponents.put("to", recipients);
            toAndComponents.put("components", components);

            // 🔹 Template
            Map<String, Object> template = new HashMap<>();
            template.put("name", "event_photos_link");
            template.put("namespace", "48851ce4_cb2d_4775_b0a6_e7d38323e124");

            Map<String, Object> language = new HashMap<>();
            language.put("code", "en");
            language.put("policy", "deterministic");

            template.put("language", language);
            template.put("to_and_components", List.of(toAndComponents));

            // 🔹 Payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("messaging_product", "whatsapp");
            payload.put("type", "template");
            payload.put("template", template);

            // 🔹 Final Body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("integrated_number", "15559290843");
            requestBody.put("content_type", "template");
            requestBody.put("payload", payload);

            // 🔹 API Call
            kong.unirest.HttpResponse<String> response = Unirest
                    .post("https://api.msg91.com/api/v5/whatsapp/whatsapp-outbound-message/bulk/")
                    .header("Content-Type", "application/json").header("authkey", "317952AtUwDOh2wv5e43d16cP1")
                    .body(requestBody).asString();

            System.out.println("Photo delivery WA_SEND | name=" + body1 + " | mobiles=" + recipients +
                    " | event=" + body2 + " | status=" + response.getStatus());
            System.out.println("Response: " + response.getBody());

        } catch (Exception e) {
            e.printStackTrace();
            // return "ERROR: " + e.getMessage();
        }
    }

    public static void eventCreated(List<String> recipients, String body1, String body2, String body3,
            String body4, String body5) {

        try {

            // 🔹 Components (template variables)
            Map<String, Object> components = new HashMap<>();
            components.put("body_1", Map.of("type", "text", "value", body1));
            components.put("body_2", Map.of("type", "text", "value", body2));
            components.put("body_3", Map.of("type", "text", "value", body3));
            components.put("body_4", Map.of("type", "text", "value", body4));
            components.put("body_5", Map.of("type", "text", "value", body5));

            // 🔹 to_and_components
            Map<String, Object> toAndComponents = new HashMap<>();
            toAndComponents.put("to", recipients);
            toAndComponents.put("components", components);

            // 🔹 Template
            Map<String, Object> template = new HashMap<>();
            template.put("name", "event_created_new1");
            template.put("namespace", "48851ce4_cb2d_4775_b0a6_e7d38323e124");

            Map<String, Object> language = new HashMap<>();
            language.put("code", "en");
            language.put("policy", "deterministic");

            template.put("language", language);
            template.put("to_and_components", List.of(toAndComponents));

            // 🔹 Payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("messaging_product", "whatsapp");
            payload.put("type", "template");
            payload.put("template", template);

            // 🔹 Final Body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("integrated_number", "15559290843");
            requestBody.put("content_type", "template");
            requestBody.put("payload", payload);

            // 🔹 API Call
            kong.unirest.HttpResponse<String> response = Unirest
                    .post("https://api.msg91.com/api/v5/whatsapp/whatsapp-outbound-message/bulk/")
                    .header("Content-Type", "application/json").header("authkey", "317952AtUwDOh2wv5e43d16cP1")
                    .body(requestBody).asString();

            System.out.println("event create WA_SEND | name=" + body1 + " | mobiles=" + recipients +
                    " | event=" + body2 + " | status=" + response.getStatus());
            System.out.println("Response: " + response.getBody());

        } catch (Exception e) {
            e.printStackTrace();
            // return "ERROR: " + e.getMessage();
        }
    }

}
