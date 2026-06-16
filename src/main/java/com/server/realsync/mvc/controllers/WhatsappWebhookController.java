package com.server.realsync.mvc.controllers;

import java.time.LocalDateTime;
import java.util.Map;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.server.realsync.entity.*;
import com.server.realsync.services.*;

@RestController
@RequestMapping("/api/webhooks/whatsapp")
public class WhatsappWebhookController {

    private final MessageQueueService queueService;
    private final ScheduleEntryService scheduleEntryService;
    private final ScheduleExecutionLogService logService;

    public WhatsappWebhookController(
            MessageQueueService queueService,
            ScheduleEntryService scheduleEntryService,
            ScheduleExecutionLogService logService) {
        this.queueService = queueService;
        this.scheduleEntryService = scheduleEntryService;
        this.logService = logService;
    }

    @PostMapping("/msg91")
    public ResponseEntity<?> handleMsg91Callback(@RequestBody String payloadString) {
        try {
            if (payloadString == null || payloadString.isBlank()) {
                return ResponseEntity.badRequest().body("Empty payload");
            }

            JSONObject json = new JSONObject(payloadString);
            String requestId = null;
            String status = null;

            // MSG91 Webhook standard formats extraction
            if (json.has("request_id")) {
                requestId = json.getString("request_id");
            } else if (json.has("requestId")) {
                requestId = json.getString("requestId");
            } else if (json.has("message_id")) {
                requestId = json.getString("message_id");
            }

            if (json.has("status")) {
                status = json.getString("status");
            }

            if (requestId == null || requestId.isBlank()) {
                return ResponseEntity.ok(Map.of("message", "Callback ignored: message id missing"));
            }

            // Find MessageQueue job
            MessageQueue job = queueService.findByMessageId(requestId).orElse(null);
            if (job == null) {
                return ResponseEntity.ok(Map.of("message", "Callback ignored: message id not found in queue"));
            }

            // Duplicate safety: Ignore if already DELIVERED
            if (job.getStatus() == QueueStatus.DELIVERED) {
                return ResponseEntity.ok(Map.of("message", "Callback ignored: duplicate status"));
            }

            // Check status for delivery confirmation
            if ("delivered".equalsIgnoreCase(status) || "DELIVERED".equalsIgnoreCase(status) || status == null) {
                job.setStatus(QueueStatus.DELIVERED);
                job.setDeliveredAt(LocalDateTime.now());
                queueService.save(job);

                // Update ScheduleEntry execution status if applicable
                ScheduleEntry entry = scheduleEntryService.getById(job.getEntityEntryId()).orElse(null);
                if (entry != null) {
                    entry.setExecutionStatus(ExecutionStatus.SUCCESS);
                    scheduleEntryService.save(entry);

                    // Log audit trail
                    ScheduleExecutionLog log = new ScheduleExecutionLog();
                    log.setScheduleEntryId(entry.getId());
                    log.setChannel(Channel.WHATSAPP);
                    log.setStatus(ExecutionResult.DELIVERED);
                    log.setResponse("WhatsApp delivered to customer");
                    logService.save(log);

                    System.out.println(String.format(
                            "WA_DELIVERED | scheduleEntryId=%d | customerId=%s | mobile=%s | messageId=%s | retryCount=%d | queueStatus=%s",
                            entry.getId(), entry.getCustomerId() != null ? String.valueOf(entry.getCustomerId()) : "N/A", "N/A", requestId, job.getRetryCount(), "DELIVERED"
                    ));
                }
            }

            return ResponseEntity.ok(Map.of("status", "processed"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
