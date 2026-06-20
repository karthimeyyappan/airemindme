package com.server.realsync.mvc.controllers;

import com.server.realsync.entity.*;
import com.server.realsync.services.*;
import com.server.realsync.repo.PromotionItemRepository;
import com.server.realsync.repo.PromotionExecutionLogRepository;
import com.server.realsync.dto.PromotionResponseDTO;
import com.server.realsync.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    private static final Logger logger = LoggerFactory.getLogger(PromotionController.class);

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${app.public.base-url:https://numen.uno}")
    private String publicBaseUrl;

    @Autowired
    private PromotionService promotionService;

    @Autowired
    private PromotionEntryService entryService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private PromotionItemRepository promotionItemRepository;

    @Autowired
    private PromotionExecutionLogRepository promotionExecutionLogRepository;

    @Autowired
    private RealSyncWhatsappService realSyncWhatsappService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private CatlogPlanService settingsPlanService;

    @Autowired
    private CatalogProductService catalogProductService;

    /**
     * Creates a promotion, stores selected catalog items, and generates entries for
     * all targeted customers.
     */
    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@RequestBody PromotionRequest request) {
        try {
            logger.info("STEP 4 - Backend Request Received: request={}", request);
            Account accountStub = SecurityUtil.getCurrentAccountId();
            if (accountStub == null || accountStub.getId() == 0) {
                logger.warn("STEP 4 - Backend Request Received: Unauthorized user access attempt");
                return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
            }
            Account account = accountService.getById(accountStub.getId());
            if (account == null) {
                logger.warn("STEP 4 - Backend Request Received: Account not found");
                return ResponseEntity.status(404).body(Map.of("message", "Account not found"));
            }

            // Defensive validations for business details
            String busName = account.getBusinessName() != null ? account.getBusinessName().trim() : "";
            String busPhone = account.getBusinessPhone() != null ? account.getBusinessPhone().trim() : "";

            if (busName.isEmpty()) {
                logger.warn("STEP 4 - Backend Request Received: Validation failed, Business Name is empty");
                return ResponseEntity.badRequest().body(Map.of("message",
                        "Business Name is not configured in your profile. Please configure it before sending promotions."));
            }
            if (busPhone.isEmpty()) {
                logger.warn("STEP 4 - Backend Request Received: Validation failed, Business Phone is empty");
                return ResponseEntity.badRequest().body(Map.of("message",
                        "Business Phone is not configured in your profile. Please configure it before sending promotions."));
            }

            // Validation Rules (10)
            if (request.description() == null || request.description().trim().isEmpty()) {
                logger.warn("STEP 4 - Backend Request Received: Validation failed, Description is empty");
                return ResponseEntity.badRequest().body(Map.of("message", "Description cannot be empty."));
            }
            if (request.itemIds() == null || request.itemIds().isEmpty()) {
                logger.warn("STEP 4 - Backend Request Received: Validation failed, Item IDs list is empty");
                return ResponseEntity.badRequest().body(Map.of("message", "At least one item must be selected."));
            }
            if (request.groupId() == null && request.customerId() == null) {
                logger.warn("STEP 4 - Backend Request Received: Validation failed, Recipient not selected");
                return ResponseEntity.badRequest().body(Map.of("message", "No recipient selected."));
            }

            LocalDateTime sched = null;
            if (request.scheduledAt() != null && !request.scheduledAt().trim().isEmpty()) {
                try {
                    sched = LocalDateTime.parse(request.scheduledAt());
                } catch (Exception e) {
                    logger.warn("STEP 4 - Backend Request Received: Validation failed, invalid schedule date {}",
                            request.scheduledAt());
                    return ResponseEntity.badRequest().body(Map.of("message", "Invalid schedule date."));
                }
                if (sched.isBefore(LocalDateTime.now())) {
                    logger.warn("STEP 4 - Backend Request Received: Validation failed, schedule date in the past");
                    return ResponseEntity.badRequest().body(Map.of("message", "Scheduled date cannot be in the past."));
                }
            }

            // Step 1: Create Promotion
            logger.info("STEP 5 - Promotion Entity Created: Preparing entity fields");
            Promotion p = new Promotion();
            p.setAccountId(account.getId());
            p.setCustomerGroupId(request.groupId());
            p.setDescription(request.description());
            p.setImageUrl("");
            p.setType("MANUAL");
            p.setStatus(sched != null ? "SCHEDULED" : "ACTIVE");
            p.setScheduledAt(sched);
            p.setCreatedAt(LocalDateTime.now());
            p.setTemplateName(request.templateName());
            p.setTemplateVariant(request.templateVariant());
            p.setAiGeneratedTitle(request.aiGeneratedTitle());
            p.setAiWhatsappContent(request.aiWhatsappContent());
            p.setAiBlogContent(request.aiBlogContent());

            logger.info("STEP 6 - Saving Promotion (before save): description length={}", p.getDescription().length());
            Promotion saved = promotionService.save(p);
            logger.info("STEP 6 - Promotion Saved (after save): promotionId={}", saved.getId());

            // Step 2: Save Promotion Items
            if (request.itemIds() != null) {
                logger.info("STEP 7 - Saving Promotion Items: Incoming count={}", request.itemIds().size());
                for (String compositeId : request.itemIds()) {
                    String[] parts = compositeId.split("-");
                    if (parts.length >= 2) {
                        String type = parts[0];
                        try {
                            Integer itemId = Integer.parseInt(parts[1]);
                            logger.info("STEP 7 - Saving Promotion Item (before save): itemId={}, type={}", itemId,
                                    type);
                            PromotionItem pi = new PromotionItem(saved.getId(), itemId, type);
                            promotionItemRepository.save(pi);
                            logger.info("STEP 7 - Promotion Item Saved: piId={}", pi.getId());
                        } catch (NumberFormatException e) {
                            logger.error("STEP 7 - Saving Promotion Items: Failed to parse itemId from parts[1]: {}",
                                    parts[1], e);
                        }
                    } else {
                        logger.error("STEP 7 - Saving Promotion Items: Invalid compositeId format: {}", compositeId);
                    }
                }
            } else {
                logger.warn("STEP 7 - Saving Promotion Items: Incoming list is NULL");
            }

            // Determine recipients and Step 3: Generate Promotion Entries
            List<Customer> customers = new ArrayList<>();
            if (request.customerId() != null) {
                Customer customer = customerService
                        .getById(account.getId(), request.customerId())
                        .orElse(null);
                if (customer != null) {
                    customers.add(customer);
                }
            } else if (request.groupId() != null) {
                customers = customerService
                        .getByAccountAndGroup(account.getId(), request.groupId(), Pageable.unpaged())
                        .getContent();
            }

            if (customers.isEmpty()) {
                logger.warn("STEP 8 - Saving Promotion Entries: No customers found for recipient selection");
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "No active customers found for the selection."));
            }

            // Generate actual URL and replace placeholder
            String link = publicBaseUrl + "/promo/" + saved.getId();

            if (link.contains("localhost")) {
                logger.error(
                        "STEP 4 - Backend Request Received: Validation failed, public URL contains localhost: link={}",
                        link);
                return ResponseEntity.status(500).body(
                        Map.of("message", "Internal server configuration error: public base URL cannot be localhost."));
            }

            saved.setPromotionUrl(link);

            if (saved.getAiWhatsappContent() != null) {
                saved.setAiWhatsappContent(saved.getAiWhatsappContent().replace("{PROMOTION_URL}", link));
            }
            if (saved.getAiBlogContent() != null) {
                saved.setAiBlogContent(saved.getAiBlogContent().replace("{PROMOTION_URL}", link));
            }
            if (saved.getDescription() != null) {
                saved.setDescription(saved.getDescription().replace("{PROMOTION_URL}", link));
            }

            // Re-save to persist the actual URL and replaced content
            logger.info("STEP 6 - Re-saving Promotion with URL (before save): id={}, URL={}", saved.getId(), link);
            saved = promotionService.save(saved);
            logger.info("STEP 6 - Re-saving Promotion with URL (after save): id={}", saved.getId());

            logger.info("STEP 8 - Saving Promotion Entries: Target count={}", customers.size());
            boolean isSendNow = (sched == null);
            int sentCount = 0;
            int failedCount = 0;

            // Save entries and logs
            for (Customer c : customers) {
                logger.info("STEP 8 - Saving Promotion Entry (before save): customerId={}", c.getId());
                PromotionEntry entry = new PromotionEntry();
                entry.setPromotionId(saved.getId());
                entry.setCustomerId(c.getId());
                entry.setTriggeredDate(LocalDateTime.now());
                entry.setStatus("PENDING");

                PromotionEntry savedEntry = entryService.save(entry);
                logger.info("STEP 8 - Promotion Entry Saved: entryId={}", savedEntry.getId());

                // Step 4: Generate Execution Logs
                logger.info("STEP 8 - Saving Promotion Execution Log (before save): entryId={}", savedEntry.getId());
                PromotionExecutionLog log = new PromotionExecutionLog();
                log.setPromotionEntryId(savedEntry.getId());

                Channel ch = Channel.WHATSAPP;
                if (request.sendVia() != null) {
                    if ("sms".equalsIgnoreCase(request.sendVia()))
                        ch = Channel.SMS;
                    else if ("email".equalsIgnoreCase(request.sendVia()) || "em".equalsIgnoreCase(request.sendVia()))
                        ch = Channel.EMAIL;
                }
                log.setChannel(ch);

                if (isSendNow && ch == Channel.WHATSAPP) {
                    // Send Now Flow using existing Invoice WhatsApp sender
                    String mobile = c.getMobile();
                    if (mobile == null || mobile.isBlank()) {
                        log.setStatus(ExecutionResult.FAILED);
                        String errMsg = "Customer mobile number is missing";
                        log.setResponse(errMsg);

                        savedEntry.setStatus("FAILED");
                        savedEntry.setFailureReason(errMsg);
                        entryService.save(savedEntry);
                        failedCount++;
                    } else {
                        try {
                            String custName = c.getName() != null ? c.getName() : "Customer";
                            String content = saved.getAiWhatsappContent() != null ? saved.getAiWhatsappContent()
                                    : "Hi {NAME}, check out our new promotion: {PROMOTION_URL}"
                                            .replace("{NAME}", custName).replace("{PROMOTION_URL}", link);
                            logger.info(
                                    "CONTROLLER - SENDING WHATSAPP NOW: mobile={}, customerName={}, businessName={}, businessMobile={}, content={}",
                                    mobile, custName, busName, busPhone, content);
                            kong.unirest.HttpResponse<String> response = realSyncWhatsappService.sendReminderTemplate(
                                    mobile,
                                    custName,
                                    content,
                                    busName,
                                    busPhone);

                            if (response.getStatus() == 200) {
                                log.setStatus(ExecutionResult.SENT);
                                log.setResponse("WhatsApp message sent successfully: " + response.getBody());
                                savedEntry.setSentWhatsapp(true);
                                savedEntry.setStatus("SENT");
                                savedEntry.setSentAt(LocalDateTime.now());
                                savedEntry.setFailureReason(null);
                                entryService.save(savedEntry);
                                sentCount++;
                            } else {
                                log.setStatus(ExecutionResult.FAILED);
                                String errMsg = "Failed to send: HTTP " + response.getStatus() + " - "
                                        + response.getBody();
                                log.setResponse(errMsg);
                                savedEntry.setStatus("FAILED");
                                savedEntry.setFailureReason(errMsg);
                                entryService.save(savedEntry);
                                failedCount++;
                            }
                        } catch (Exception ex) {
                            logger.error("Failed to send WhatsApp message to {}", mobile, ex);
                            log.setStatus(ExecutionResult.FAILED);
                            String errMsg = "Failed to send: " + ex.getMessage();
                            log.setResponse(errMsg);
                            savedEntry.setStatus("FAILED");
                            savedEntry.setFailureReason(errMsg);
                            entryService.save(savedEntry);
                            failedCount++;
                        }
                    }
                } else {
                    log.setStatus(ExecutionResult.PENDING);
                    log.setResponse("Pending execution");
                    savedEntry.setStatus("PENDING");
                    entryService.save(savedEntry);
                }

                promotionExecutionLogRepository.save(log);
                logger.info("STEP 8 - Promotion Execution Log Saved: logId={}", log.getId());
            }

            // Return expected success response structure
            logger.info(
                    "STEP 9 - Response Returned: success=true, promotionId={}, url={}, sentCount={}, failedCount={}",
                    saved.getId(), link, sentCount, failedCount);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "promotionId", saved.getId(),
                    "promotionUrl", link,
                    "sentCount", sentCount,
                    "failedCount", failedCount,
                    "totalCount", customers.size()));
        } catch (Exception e) {
            logger.error("API FAILED - Exception in Promotion creation: request={}", request, e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Failed to create promotion: " + e.getMessage()));
        }
    }

    @PostMapping("/generate-content")
    public ResponseEntity<?> generateContent(@RequestBody Map<String, Object> request) {
        Account accountStub = SecurityUtil.getCurrentAccountId();
        if (accountStub == null || accountStub.getId() == 0) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        Account account = accountService.getById(accountStub.getId());
        if (account == null) {
            return ResponseEntity.status(404).body("Account not found");
        }

        String description = (String) request.get("description");
        List<String> itemIds = (List<String>) request.get("itemIds");

        String itemsStr = "None selected";
        if (itemIds != null && !itemIds.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (String compositeId : itemIds) {
                String[] parts = compositeId.split("-");
                if (parts.length >= 2) {
                    try {
                        Integer itemId = Integer.parseInt(parts[1]);
                        if ("plan".equalsIgnoreCase(parts[0])) {
                            settingsPlanService.getById(itemId).ifPresent(p -> names.add("Plan: " + p.getName()));
                        } else {
                            catalogProductService.getById(itemId, account.getId())
                                    .ifPresent(p -> names.add("Product: " + p.getName()));
                        }
                    } catch (Exception e) {
                    }
                }
            }
            if (!names.isEmpty()) {
                itemsStr = String.join(", ", names);
            }
        }

        try {
            if (apiKey == null || apiKey.isBlank() || apiKey.equals("YOUR_GEMINI_API_KEY")) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Gemini API key is not configured in application properties.");
            }

            String actualApiKey = apiKey;

            if (actualApiKey.length() > 10) {
                actualApiKey = actualApiKey.substring(5);
            }


            String prompt = String.format(
                    """
                            You are an AI assistant generating promotional campaign content for a business.

                            Business Context:
                            Business Name: %s
                            Business Category: %s
                            Business Subcategory: %s

                            Selected Catalog Items:
                            %s

                            Instructions/Description:
                            %s

                            Rules for generation:
                            1. Generate exactly four distinct fields:
                               - title: A catchy promotion title (3-6 words).
                               - aiWhatsappContent: A very short WhatsApp message (16-20 words). It MUST include the exact placeholder {PROMOTION_URL} to represent the link. Do not include signatures, greetings, or customer names in this WhatsApp message.
                               - description: Complete landing page description content (100-500 words) detailing the offer, benefits, catalog items, and a call-to-action.
                               - aiBlogContent: Email marketing copy (100-300 words) with a professional subject line and structured body layout.
                            2. Keep the WhatsApp message under 20 words.
                            3. Return ONLY a raw JSON object containing these exact keys. Do not wrap it in markdown or code blocks.

                            Format:
                            {
                              "title": "...",
                              "aiWhatsappContent": "...",
                              "description": "...",
                              "aiBlogContent": "..."
                            }
                            """,
                    account.getBusinessName() != null ? account.getBusinessName() : "",
                    account.getCategory() != null ? account.getCategory() : "",
                    account.getSubcategory() != null ? account.getSubcategory() : "",
                    itemsStr,
                    description != null ? description : "");

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

            HttpClient client = HttpClient.newHttpClient();
            String[] models = {
                    "gemini-2.0-flash",
                    "gemini-3.1-flash-lite",
                    "gemini-2.5-flash",
                    "gemini-1.5-flash"
            };

            String generatedContent = null;
            HttpResponse<String> lastHttpResponse = null;

            for (String model : models) {
                try {
                    HttpRequest httpRequest = HttpRequest.newBuilder()
                            .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + model
                                    + ":generateContent?key=" + actualApiKey))
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
                }
            }

            if (generatedContent == null) {
                String errorBody = lastHttpResponse != null ? lastHttpResponse.body() : "No response from Gemini API";
                return ResponseEntity.status(502).body("Error from Gemini API: " + errorBody);
            }

            generatedContent = generatedContent.trim();
            if (generatedContent.startsWith("```")) {
                generatedContent = generatedContent.replaceAll("^```[a-zA-Z]*\\n", "");
                generatedContent = generatedContent.replaceAll("\\n```$", "");
                generatedContent = generatedContent.trim();
            }

            JSONObject resultJson = new JSONObject(generatedContent);
            return ResponseEntity.ok(resultJson.toMap());

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error generating content: " + e.getMessage());
        }
    }

    @GetMapping
    public List<PromotionResponseDTO> getAll() {
        Account account = SecurityUtil.getCurrentAccountId();
        if (account == null) {
            return new ArrayList<>();
        }
        List<Promotion> promotions = promotionService.getByAccount(account.getId());
        return promotions.stream().map(p -> {
            List<PromotionEntry> entries = entryService.getByPromotion(p.getId());
            long recipientCount = entries.size();
            long totalViews = entries.stream().mapToLong(e -> e.getViewCount() != null ? e.getViewCount() : 0L).sum();
            long totalLikes = entries.stream().mapToLong(e -> e.getLikeCount() != null ? e.getLikeCount() : 0L).sum();
            long totalEnquiries = entries.stream()
                    .mapToLong(e -> e.getEnquiryCount() != null ? e.getEnquiryCount() : 0L).sum();
            long firstEntryId = entries.isEmpty() ? 0L : entries.get(0).getId();

            List<PromotionItem> items = promotionItemRepository.findByPromotionId(p.getId());
            List<String> itemNames = items.stream().map(item -> {
                if ("plan".equalsIgnoreCase(item.getItemType())) {
                    return settingsPlanService.getById(item.getItemId())
                            .map(CatalogPlan::getName)
                            .orElse("Plan #" + item.getItemId());
                } else {
                    return catalogProductService.getById(item.getItemId(), account.getId())
                            .map(CatalogProduct::getName)
                            .orElse("Product #" + item.getItemId());
                }
            }).collect(Collectors.toList());

            String name = p.getAiGeneratedTitle() != null && !p.getAiGeneratedTitle().isEmpty()
                    ? p.getAiGeneratedTitle()
                    : (p.getDescription().length() > 30 ? p.getDescription().substring(0, 30) + "..."
                            : p.getDescription());

            return new PromotionResponseDTO(
                    p.getId(),
                    name,
                    p.getDescription(),
                    itemNames,
                    recipientCount,
                    p.getStatus() != null ? p.getStatus() : "ACTIVE",
                    totalViews,
                    totalLikes,
                    totalEnquiries,
                    p.getCreatedAt(),
                    p.getScheduledAt(),
                    firstEntryId);
        }).collect(Collectors.toList());
    }

    @GetMapping("/public/{promotionId}")
    public ResponseEntity<Map<String, Object>> getPublicPromoLanding(@PathVariable Long promotionId,
            @RequestParam(value = "entry", required = false) Long entryId) {
        Promotion promo = promotionService.getById(promotionId).orElse(null);
        if (promo == null) {
            return ResponseEntity.notFound().build();
        }

        List<PromotionEntry> entries = entryService.getByPromotion(promo.getId());
        long viewCount = entries.stream().mapToLong(e -> e.getViewCount() != null ? e.getViewCount() : 0L).sum();
        long likeCount = entries.stream().mapToLong(e -> e.getLikeCount() != null ? e.getLikeCount() : 0L).sum();
        long enquiryCount = entries.stream().mapToLong(e -> e.getEnquiryCount() != null ? e.getEnquiryCount() : 0L)
                .sum();

        // Find customer name and actual entry ID for tracking
        String customerName = "Customer";
        Long targetEntryId = null;
        if (entryId != null) {
            PromotionEntry entry = entryService.getById(entryId).orElse(null);
            if (entry != null && entry.getPromotionId().equals(promo.getId())) {
                targetEntryId = entry.getId();
                Customer customer = customerService.getById(promo.getAccountId(), entry.getCustomerId()).orElse(null);
                if (customer != null) {
                    customerName = customer.getName();
                }
            }
        }

        if (targetEntryId == null && !entries.isEmpty()) {
            targetEntryId = entries.get(0).getId();
            Customer customer = customerService.getById(promo.getAccountId(), entries.get(0).getCustomerId())
                    .orElse(null);
            if (customer != null) {
                customerName = customer.getName();
            }
        }

        List<PromotionItem> promotionItems = promotionItemRepository.findByPromotionId(promo.getId());
        List<Map<String, Object>> normalizedItems = promotionItems.stream().map(item -> {
            Map<String, Object> map = new java.util.HashMap<>();
            if ("plan".equalsIgnoreCase(item.getItemType())) {
                CatalogPlan plan = settingsPlanService.getById(item.getItemId()).orElse(null);
                if (plan != null) {
                    map.put("id", "plan-" + plan.getId());
                    map.put("type", "PLAN");
                    map.put("name", plan.getName());
                    map.put("price", "₹" + (plan.getPrice() != null ? plan.getPrice() : 0));
                    map.put("priceNote", plan.getBillingCycle() != null ? " / " + plan.getBillingCycle() : "");
                    map.put("desc", plan.getDescription() != null ? plan.getDescription() : "");
                    map.put("features",
                            plan.getFeatures() != null ? List.of(plan.getFeatures().split(",")) : new ArrayList<>());
                    map.put("img", plan.getImageUrl() != null && plan.getImageUrl().length() > 5
                            ? "/doc/view?path=" + java.net.URLEncoder.encode(plan.getImageUrl().replaceFirst("^/+", ""),
                                    java.nio.charset.StandardCharsets.UTF_8)
                            : "https://images.unsplash.com/photo-1554224155-6726b3ff858f?w=400&q=80");
                }
            } else {
                CatalogProduct prod = catalogProductService.getById(item.getItemId(), promo.getAccountId())
                        .orElse(null);
                if (prod != null) {
                    map.put("id", "product-" + prod.getId());
                    map.put("type", "PRODUCT");
                    map.put("name", prod.getName());
                    map.put("price", "₹" + (prod.getPrice() != null ? prod.getPrice() : 0));
                    map.put("priceNote", "");
                    map.put("code", prod.getSku() != null ? prod.getSku() : "N/A");
                    map.put("desc", prod.getDescription() != null ? prod.getDescription() : "");
                    map.put("features", new ArrayList<>());
                    map.put("img", prod.getImageUrl() != null && prod.getImageUrl().length() > 5
                            ? "/doc/view?path=" + java.net.URLEncoder.encode(prod.getImageUrl().replaceFirst("^/+", ""),
                                    java.nio.charset.StandardCharsets.UTF_8)
                            : "https://images.unsplash.com/photo-1512428559087-560fa5ceab42?w=400&q=80");
                }
            }
            return map;
        }).filter(m -> !m.isEmpty()).collect(Collectors.toList());

        Map<String, Object> accountMap = new java.util.HashMap<>();
        if (accountService != null) {
            Account act = accountService.getById(promo.getAccountId());
            if (act != null) {
                accountMap.put("name", act.getBusinessName() != null ? act.getBusinessName() : "Numen");
                accountMap.put("phone", act.getBusinessPhone() != null ? act.getBusinessPhone() : "ERROR: No Phone");
                accountMap.put("email", act.getBusinessEmail() != null ? act.getBusinessEmail() : "ERROR: No Email");
            }
        }

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("entryId", targetEntryId);
        response.put("promotionId", promo.getId());
        response.put("promotionTitle",
                promo.getAiGeneratedTitle() != null && !promo.getAiGeneratedTitle().isEmpty()
                        ? promo.getAiGeneratedTitle()
                        : "Exclusive Offers");
        response.put("promotionDescription", promo.getDescription());
        response.put("customerName", customerName);
        response.put("items", normalizedItems);
        response.put("account", accountMap);
        response.put("recipientCount", entries.size());
        response.put("viewCount", viewCount);
        response.put("likeCount", likeCount);
        response.put("enquiryCount", enquiryCount);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/public/{entryId}/view")
    @Transactional
    public ResponseEntity<Void> trackView(@PathVariable Long entryId) {
        PromotionEntry entry = entryService.getById(entryId).orElse(null);
        if (entry != null) {
            entry.setViewCount((entry.getViewCount() != null ? entry.getViewCount() : 0) + 1);
            if (entry.getFirstViewedAt() == null) {
                entry.setFirstViewedAt(LocalDateTime.now());
            }
            entry.setLastViewedAt(LocalDateTime.now());
            entryService.save(entry);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/public/{entryId}/like")
    @Transactional
    public ResponseEntity<Void> trackLike(@PathVariable Long entryId) {
        PromotionEntry entry = entryService.getById(entryId).orElse(null);
        if (entry != null) {
            entry.setLikeCount((entry.getLikeCount() != null ? entry.getLikeCount() : 0) + 1);
            entry.setLikedAt(LocalDateTime.now());
            entryService.save(entry);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/public/{entryId}/enquiry")
    @Transactional
    public ResponseEntity<Void> trackEnquiry(@PathVariable Long entryId) {
        PromotionEntry entry = entryService.getById(entryId).orElse(null);
        if (entry != null) {
            entry.setEnquiryCount((entry.getEnquiryCount() != null ? entry.getEnquiryCount() : 0) + 1);
            entry.setEnquiryAt(LocalDateTime.now());
            entryService.save(entry);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/public/{entryId}/whatsapp-click")
    @Transactional
    public ResponseEntity<Void> trackWhatsappClick(@PathVariable Long entryId) {
        PromotionEntry entry = entryService.getById(entryId).orElse(null);
        if (entry != null) {
            entry.setWhatsappClickCount(
                    (entry.getWhatsappClickCount() != null ? entry.getWhatsappClickCount() : 0) + 1);
            entry.setWhatsappClickedAt(LocalDateTime.now());
            entryService.save(entry);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/public/{entryId}/phone-click")
    @Transactional
    public ResponseEntity<Void> trackPhoneClick(@PathVariable Long entryId) {
        PromotionEntry entry = entryService.getById(entryId).orElse(null);
        if (entry != null) {
            entry.setPhoneClickCount((entry.getPhoneClickCount() != null ? entry.getPhoneClickCount() : 0) + 1);
            entry.setPhoneClickedAt(LocalDateTime.now());
            entryService.save(entry);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/public/{entryId}/email-click")
    @Transactional
    public ResponseEntity<Void> trackEmailClick(@PathVariable Long entryId) {
        PromotionEntry entry = entryService.getById(entryId).orElse(null);
        if (entry != null) {
            entry.setEmailClickCount((entry.getEmailClickCount() != null ? entry.getEmailClickCount() : 0) + 1);
            entry.setEmailClickedAt(LocalDateTime.now());
            entryService.save(entry);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{promotionId}/analytics")
    public ResponseEntity<Map<String, Object>> getPromotionAnalytics(@PathVariable Long promotionId) {
        List<PromotionEntry> entries = entryService.getByPromotion(promotionId);
        long totalViews = entries.stream().mapToLong(e -> e.getViewCount() != null ? e.getViewCount() : 0L).sum();
        long totalLikes = entries.stream().mapToLong(e -> e.getLikeCount() != null ? e.getLikeCount() : 0L).sum();
        long totalEnquiries = entries.stream().mapToLong(e -> e.getEnquiryCount() != null ? e.getEnquiryCount() : 0L)
                .sum();
        long totalWhatsappClicks = entries.stream()
                .mapToLong(e -> e.getWhatsappClickCount() != null ? e.getWhatsappClickCount() : 0L).sum();
        long totalPhoneClicks = entries.stream()
                .mapToLong(e -> e.getPhoneClickCount() != null ? e.getPhoneClickCount() : 0L).sum();
        long totalEmailClicks = entries.stream()
                .mapToLong(e -> e.getEmailClickCount() != null ? e.getEmailClickCount() : 0L).sum();

        Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("promotionId", promotionId);
        summary.put("recipientCount", entries.size());
        summary.put("totalViews", totalViews);
        summary.put("totalLikes", totalLikes);
        summary.put("totalEnquiries", totalEnquiries);
        summary.put("totalWhatsappClicks", totalWhatsappClicks);
        summary.put("totalPhoneClicks", totalPhoneClicks);
        summary.put("totalEmailClicks", totalEmailClicks);

        return ResponseEntity.ok(summary);
    }
}

/**
 * Data Transfer Object (DTO) to handle the incoming JSON payload safely.
 */
record PromotionRequest(
        Integer groupId,
        Integer customerId,
        String description,
        List<String> itemIds,
        String sendVia,
        String scheduledAt,
        String templateName,
        String templateVariant,
        String aiGeneratedTitle,
        String aiWhatsappContent,
        String aiBlogContent) {
}
