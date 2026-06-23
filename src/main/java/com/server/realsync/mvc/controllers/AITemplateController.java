package com.server.realsync.mvc.controllers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.server.realsync.dto.TemplateGenerateRequest;
import com.server.realsync.dto.TemplateGenerateResponse;
import com.server.realsync.entity.Account;
import com.server.realsync.util.SecurityUtil;

@RestController
@RequestMapping("/api/ai")
public class AITemplateController {

    @Value("${gemini.api.key}")
    private String apiKey;

    @PostMapping("/template/generate")
    public ResponseEntity<?> generateTemplate(@RequestBody TemplateGenerateRequest request) {
        try {
            Account account = SecurityUtil.getCurrentAccountId();
            if (account == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }

            if (apiKey == null || apiKey.isBlank() || apiKey.equals("YOUR_GEMINI_API_KEY")) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Gemini API key is not configured.");
            }

            String actualApiKey = apiKey;

            if (actualApiKey.length() > 10) {
                actualApiKey = actualApiKey.substring(5);
            }

            

            // Determine variant count
            int count = request.getVariantCount() != null ? request.getVariantCount() : 1;

            // Build Prompt
            String prompt = String.format(
                    """
                            You are an AI assistant generating content for a WhatsApp approved template.

                            The system uses only two WhatsApp templates.

                            Template 1: Reminder Template
                            Hello {{1}} 👋,
                            {{2}}
                            Thanks,
                            {{3}}
                            📞 {{4}}
                            Thank you for trusting us

                            Template 2: Document / URL Template
                            Hello {{1}} 👋,
                            {{2}}
                            📄 Reference Number: {{3}}
                            View Details:
                            {{4}}
                            If you have any questions or require assistance, please contact us.
                            Thanks,
                            {{5}}
                            📞 {{6}}
                            Have a great day!

                            Important:
                            Gemini must generate ONLY content for variable {{2}}.

                            Never generate:
                            * Hello
                            * Hi
                            * Greetings
                            * Customer name
                            * Business name
                            * Phone number
                            * Thanks
                            * Regards
                            * Signatures
                            * Contact information
                            * URLs
                            * Reference numbers
                            * Emojis
                            * Markdown
                            * Titles
                            * Headings
                            These are already provided by the WhatsApp templates.

                            Business Context:
                            Business Name: %s
                            Business Category: %s
                            Business Subcategory: %s

                            Request Context:
                            Title: %s
                            Description: %s
                            Purpose: %s
                            Template Type: %s
                            Language: %s

                            User Requirement:
                            %s

                            Rules:
                            1. Generate only plain message content.
                            2. Output must fit inside WhatsApp variable {{2}}.
                            3. Keep content between 20 and 120 words.
                            4. Use a natural human tone.
                            5. Make content specific to the business category.
                            6. Include action-oriented language when appropriate.
                            7. Do not repeat business name.
                            8. Do not include greetings or closing lines.
                            9. Do not include placeholders.
                            10. Do not include quotation marks.
                            11. Do not explain the message.
                            12. Return only the final content.
                            13. The generated content must be directly usable inside a WhatsApp approved template.
                            14. Do not mention variable names.
                            15. Do not mention template names.
                            16. Do not generate greetings or signatures.
                            17. Return plain text only.

                            Supported Dynamic Data:
                            {amount}
                            {due_date}
                            {plan_name}
                            {document_type}
                            {reference_number}

                            Examples:
                            Payment Reminder:
                            A payment of {amount} is due on {due_date}. Kindly complete it on time to avoid any interruption in your service. If payment has already been made, please ignore this reminder.

                            Medicine Refill Reminder:
                            Your monthly medicine refill is due today. Staying on schedule helps ensure the best results from your treatment. If you need assistance, our team is ready to help.

                            Membership Renewal:
                            Your membership plan is approaching renewal. Renew before {due_date} to continue enjoying uninterrupted benefits and services.

                            Appointment Notification:
                            Your appointment has been successfully scheduled and is ready for review.

                            Invoice Notification:
                            Your invoice has been generated and is available for viewing.

                            Promotion:
                            Special offers are currently available for you. Explore the latest deals and take advantage of exclusive savings.

                            You must return a JSON object containing exactly %d distinct variations of the message content.
                            Each variant must follow all the business rules above and be placed in a JSON array under the key "variants".

                            Format:
                            {
                              "variants": [
                                "variant 1 content",
                                "variant 2 content",
                                ...
                              ]
                            }

                            Return ONLY the raw JSON object. Do not wrap it in markdown or code blocks.
                            """,
                    account.getBusinessName() != null ? account.getBusinessName() : "",
                    account.getCategory() != null ? account.getCategory() : "",
                    account.getSubcategory() != null ? account.getSubcategory() : "",
                    request.getTitle() != null ? request.getTitle() : "",
                    request.getDescription() != null ? request.getDescription() : "",
                    request.getPurpose() != null ? request.getPurpose() : "",
                    request.getTemplateType() != null ? request.getTemplateType() : "",
                    request.getLanguage() != null ? request.getLanguage() : "",
                    request.getDescription() != null ? request.getDescription() : "",
                    count);

            // Construct JSON request for Gemini
            JSONObject textPart = new JSONObject();
            textPart.put("text", prompt);

            JSONArray partsArray = new JSONArray();
            partsArray.put(textPart);

            JSONObject contentObj = new JSONObject();
            contentObj.put("parts", partsArray);

            JSONArray contentsArray = new JSONArray();
            contentsArray.put(contentObj);

            JSONObject requestBody = new JSONObject();
            requestBody.put("contents", contentsArray);

            // Send request to Gemini API
            HttpClient client = HttpClient.newHttpClient();
            String[] models = {
                    "gemini-2.0-flash",
                    "gemini-2.0-flash-lite",
                    "gemini-flash-lite-latest",
                    "gemini-2.5-flash",
                    "gemini-1.5-flash"
            };

            String generatedContent = null;
            HttpResponse<String> lastHttpResponse = null;

            for (String model : models) {
                try {
                    HttpRequest httpRequest = HttpRequest.newBuilder()
                            .uri(URI.create(
                                    "https://generativelanguage.googleapis.com/v1beta/models/" + model
                                            + ":generateContent?key="
                                            + actualApiKey))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString(), StandardCharsets.UTF_8))
                            .build();

                    lastHttpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                    if (lastHttpResponse.statusCode() == 200) {
                        JSONObject responseJson = new JSONObject(lastHttpResponse.body());
                        generatedContent = responseJson.getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text");
                        break;
                    }
                } catch (Exception e) {
                    // Log or ignore to try the next model
                }
            }

            if (generatedContent == null) {
                String errorBody = lastHttpResponse != null ? lastHttpResponse.body() : "No response from Gemini API";
                int status = lastHttpResponse != null ? lastHttpResponse.statusCode() : 502;
                return ResponseEntity.status(status)
                        .body("Error from Gemini API: " + errorBody);
            }

            generatedContent = generatedContent.trim();
            if (generatedContent.startsWith("```")) {
                generatedContent = generatedContent.replaceAll("^```[a-zA-Z]*\\n", "");
                generatedContent = generatedContent.replaceAll("\\n```$", "");
                generatedContent = generatedContent.trim();
            }

            java.util.List<String> variantsList = new java.util.ArrayList<>();
            try {
                JSONObject responseJson = new JSONObject(generatedContent);
                if (responseJson.has("variants")) {
                    JSONArray variantsArray = responseJson.getJSONArray("variants");
                    for (int i = 0; i < variantsArray.length(); i++) {
                        variantsList.add(variantsArray.getString(i));
                    }
                }
            } catch (Exception e) {
                // Fallback if parsing fails
                variantsList.add(generatedContent);
            }

            if (variantsList.isEmpty()) {
                variantsList.add(generatedContent);
            }

            return ResponseEntity.ok(new TemplateGenerateResponse(
                    request.getTitle(),
                    request.getDescription(),
                    variantsList.size(),
                    variantsList));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal error generating template: " + e.getMessage());
        }
    }
}
