package com.server.realsync.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.server.realsync.entity.Account;
import com.server.realsync.services.FileStorageService;
import com.server.realsync.util.SecurityUtil;

@RestController
@RequestMapping("/doc")
public class FileUploadController {

    private final FileStorageService fileStorageService;

    @Value("${storage.use-cloud}")
    private boolean useCloud;

    public FileUploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * Real Sync scan file upload
     * 
     * @param accountId
     * @param userId
     * @param file
     * @return
     */
    @PostMapping("/upload/a/{accountId}/p/{promptId}/{fileName}")
    public ResponseEntity<String> uploadPromptImage(
            @PathVariable String accountId,
            @PathVariable String promptId,
            @PathVariable String fileName,
            @RequestParam("file") MultipartFile file) {
        String path = "a/" + accountId + "/p/" + promptId + "/";
        try {
            fileStorageService.uploadFile(path, file, fileName);
            return ResponseEntity.status(HttpStatus.OK).body("Prompt Image uploaded successfully.");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error uploading file.");
        }
    }

    private String generateETag(String filename) {
        // Generate a simple ETag based on the filename or a hash of the file
        // For more robust solutions, consider using file hashes or last modified
        // timestamps
        return Integer.toHexString(filename.hashCode());
    }

    @GetMapping("/get/a/{accountId}/p/{promptId}/{fileName}")
    public ResponseEntity<InputStreamResource> getLegalResearchFile(
            @PathVariable String accountId,
            @PathVariable String fileName,
            @PathVariable String promptId) {
        String path = "a/" + accountId + "/p/" + promptId + "/";
        try {
            InputStream inputStream = fileStorageService.downloadFile(path, fileName);
            // Set caching headers
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + promptId + "\"");
            headers.set(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000"); // Cache for 1 year
            headers.set(HttpHeaders.ETAG, generateETag(promptId)); // Generate or retrieve ETag
            // headers.set(HttpHeaders.CONTENT_TYPE, "image/jpeg"); // Adjust based on your
            // image type
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new InputStreamResource(inputStream));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PostMapping("/upload/greeting/{greetingId}")
    public ResponseEntity<?> uploadGreetingImage(
            @PathVariable Integer greetingId,
            @RequestParam("file") MultipartFile file) {

        try {
            // 🔥 STEP 1: confirm API hit
            System.out.println("🔥 Upload API HIT");

            // 🔥 STEP 2: check input
            System.out.println("Greeting ID: " + greetingId);
            System.out.println("File name: " + file.getOriginalFilename());
            System.out.println("File size: " + file.getSize());
            System.out.println("File empty: " + file.isEmpty());

            // ⚠️ POSSIBLE CRASH POINT
            Account account = SecurityUtil.getCurrentAccountId();

            // 🔥 STEP 3: check account
            System.out.println("Account object: " + account);

            Integer accountId = account != null ? account.getId() : null;
            System.out.println("Account ID: " + accountId);

            // ❌ If this is null → your bug is here
            if (accountId == null) {
                throw new RuntimeException("Account is NULL — SecurityUtil failed");
            }

            String fileName = java.util.UUID.randomUUID() + ".jpg";

            // 🔥 STEP 4: path check
            String path = "accounts/" + accountId + "/greetings/" + greetingId + "/";
            System.out.println("Uploading to path: " + path);

            // ⚠️ POSSIBLE CRASH POINT
            fileStorageService.uploadFile(path, file, fileName);

            // 🔥 STEP 5: success confirmation
            System.out.println("✅ Upload SUCCESS");

            String fileUrl = path + fileName;

            return ResponseEntity.ok(Map.of(
                    "message", "Uploaded successfully",
                    "url", fileUrl));

        } catch (Exception e) {
            System.out.println("❌ ERROR OCCURRED:");
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage())); // 👈 VERY IMPORTANT
        }
    }

    @GetMapping("/view/greeting")
    public ResponseEntity<InputStreamResource> viewGreetingImage(
            @RequestParam String path) {

        try {
            System.out.println("🔥 VIEW API HIT: " + path);

            int lastSlash = path.lastIndexOf('/');
            String dir = path.substring(0, lastSlash + 1);
            String fileName = path.substring(lastSlash + 1);

            InputStream inputStream = fileStorageService.downloadFile(dir, fileName);

            return ResponseEntity.ok()
                    .header("Content-Type", "image/jpeg")
                    .body(new InputStreamResource(inputStream));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    
@GetMapping("/view")
public ResponseEntity<InputStreamResource> viewFile(@RequestParam String path) {
    try {
        System.out.println("🔥 VIEW FILE: " + path);

        // 🛡️ basic validation
        if (path == null || !path.contains("/") || path.contains("..")) {
            return ResponseEntity.badRequest().build();
        }

        int lastSlash = path.lastIndexOf('/');
        String dir = path.substring(0, lastSlash + 1);
        String fileName = path.substring(lastSlash + 1);

        InputStream inputStream = fileStorageService.downloadFile(dir, fileName);

        // 🧠 simple content-type detection
        String contentType = "image/jpeg";
        if (fileName.endsWith(".png")) contentType = "image/png";
        else if (fileName.endsWith(".webp")) contentType = "image/webp";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(new InputStreamResource(inputStream));

    } catch (Exception e) {
        System.out.println("❌ VIEW ERROR:");
        e.printStackTrace();
        return ResponseEntity.notFound().build();
    }
}

    @PostMapping("/upload/plan/{planId}")
public ResponseEntity<?> uploadPlanImage(
        @PathVariable Integer planId,
        @RequestParam("file") MultipartFile file) {

    try {
        Account account = SecurityUtil.getCurrentAccountId();
        Integer accountId = account.getId();

        String fileName = java.util.UUID.randomUUID() + ".jpg";

        String path = "accounts/" + accountId + "/plans/" + planId + "/";

        fileStorageService.uploadFile(path, file, fileName);

        String fileUrl = path + fileName;

        return ResponseEntity.ok(Map.of(
                "message", "Uploaded successfully",
                "url", fileUrl));

    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage()));
    }
}
@PostMapping("/upload/product/{productId}")
public ResponseEntity<?> uploadProductImage(
        @PathVariable Integer productId,
        @RequestParam("file") MultipartFile file) {

    try {
        Account account = SecurityUtil.getCurrentAccountId();
        Integer accountId = account.getId();

        String fileName = java.util.UUID.randomUUID() + ".jpg";

        String path = "accounts/" + accountId + "/products/" + productId + "/";

        fileStorageService.uploadFile(path, file, fileName);

        String fileUrl = path + fileName;

        return ResponseEntity.ok(Map.of(
                "message", "Uploaded successfully",
                "url", fileUrl));

    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage()));
    }
}
}
