package com.server.realsync.mvc.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.server.realsync.entity.Account;
import com.server.realsync.entity.CatalogPlan;
import com.server.realsync.entity.CatalogProduct;
import com.server.realsync.entity.CatalogTemplate;
import com.server.realsync.entity.InventoryTransaction;
import com.server.realsync.entity.CatalogRTemplate;
import com.server.realsync.services.CatalogProductService;
import com.server.realsync.services.CatalogRTemplateService;
import com.server.realsync.services.CatalogTemplateService;
import com.server.realsync.services.CatlogPlanService;
import com.server.realsync.services.InventoryTransactionService;
import com.server.realsync.util.SecurityUtil;

//used for report templates

@RestController
@RequestMapping("/api/catalog")
public class CatlogContoller {

    @Autowired
    private CatlogPlanService planService;

    @Autowired
    private CatalogProductService productService;

    @Autowired
    private CatalogTemplateService templateService;

    @Autowired
    private CatalogRTemplateService rTemplateService;

    @Autowired
    private InventoryTransactionService txnService;

    // GET /api/catalog/plans
    @GetMapping("/plans")
    public Page<CatalogPlan> getPlans(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size) {
        Account account = SecurityUtil.getCurrentAccountId();
        return planService.getByAccountId(
                account.getId(),
                PageRequest.of(page, size,
                        Sort.by("id").descending()));
    }

    // POST /api/catalog/plans
    @PostMapping("/plans")
    public CatalogPlan createPlan(@RequestBody CatalogPlan plan) {
        Account account = SecurityUtil.getCurrentAccountId();
        plan.setAccountId(account.getId());
        return planService.save(plan);
    }

    // PUT /api/catalog/plans/{id}
    @PutMapping("/plans/{id}")
    public ResponseEntity<CatalogPlan> updatePlan(@PathVariable Integer id, @RequestBody CatalogPlan plan) {
        Account account = SecurityUtil.getCurrentAccountId();
        return planService.getById(id)
                .filter(existing -> existing.getAccountId().equals(account.getId()))
                .map(existing -> {
                    plan.setId(id);
                    plan.setAccountId(account.getId());
                    return ResponseEntity.ok(planService.save(plan));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/catalog/plans/{id}
    @DeleteMapping("/plans/{id}")
    public ResponseEntity<Void> deletePlan(@PathVariable Integer id) {
        Account account = SecurityUtil.getCurrentAccountId();
        return planService.getById(id)
                .filter(existing -> existing.getAccountId().equals(account.getId()))
                .map(existing -> {
                    planService.delete(id);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/plans/{id}/toggle-status")
    public ResponseEntity<CatalogPlan> togglePlanStatus(@PathVariable Integer id) {
        Account account = SecurityUtil.getCurrentAccountId();
        return planService.getById(id)
                .filter(p -> p.getAccountId().equals(account.getId()))
                .map(p -> {
                    p.setStatus("active".equals(p.getStatus()) ? "inactive" : "active");
                    return ResponseEntity.ok(planService.save(p));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/plans/{id}/image")
    public ResponseEntity<?> updatePlanImage(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {

        Account account = SecurityUtil.getCurrentAccountId();

        return planService.getById(id)
                .filter(p -> p.getAccountId().equals(account.getId()))
                .map(p -> {
                    p.setImageUrl(body.get("imageUrl"));
                    return ResponseEntity.ok(planService.save(p));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/products/search")
    public List<CatalogProduct> searchProducts(
            @RequestParam String query) {

        Account account = SecurityUtil.getCurrentAccountId();

        return productService.search(account.getId(), query);
    }

    @GetMapping("/plans/search")
    public List<CatalogPlan> searchPlans(
            @RequestParam String query) {

        Account account = SecurityUtil.getCurrentAccountId();

        return planService.search(account.getId(), query);
    }

    @GetMapping("/products")
    public Page<CatalogProduct> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size) {
        Account account = SecurityUtil.getCurrentAccountId();
        return productService.getByAccountId(account.getId(), PageRequest.of(page, size, Sort.by("id").descending()));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<?> getProduct(
            @PathVariable Integer id) {

        Account account = SecurityUtil.getCurrentAccountId();

        return productService.getById(id, account.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/products")
    public CatalogProduct createProduct(@RequestBody CatalogProduct product) {
        Account account = SecurityUtil.getCurrentAccountId();
        product.setAccountId(account.getId());
        return productService.save(product);
    }
@PutMapping("/products/{id}")
public ResponseEntity<CatalogProduct> updateProduct(
        @PathVariable Integer id,
        @RequestBody CatalogProduct product) {

    Account account = SecurityUtil.getCurrentAccountId();

    return productService.getById(id, account.getId())
            .map(existing -> {

                Integer oldQty = existing.getQuantity() == null
                        ? 0
                        : existing.getQuantity();

                Integer newQty = product.getQuantity() == null
                        ? 0
                        : product.getQuantity();

                // Update Product
                existing.setName(product.getName());
                existing.setSku(product.getSku());
                existing.setCategory(product.getCategory());
                existing.setCurrency(product.getCurrency());
                existing.setPrice(product.getPrice());
                existing.setQuantity(newQty);
                existing.setDescription(product.getDescription());
                existing.setStatus(product.getStatus());

                CatalogProduct savedProduct =
                        productService.save(existing);

                // Create inventory transaction if quantity changed
                if (!oldQty.equals(newQty)) {

                    int diff = newQty - oldQty;

                    InventoryTransaction txn =
                            new InventoryTransaction();

                    txn.setAccountId(account.getId());
                    txn.setProductId(savedProduct.getId());
                    txn.setType("ADJUSTMENT");
                    txn.setQuantity(diff);
                    txn.setBalanceAfter(newQty);
                    txn.setReferenceNo("PRODUCT-EDIT");

                    txn.setNotes(
                            "Stock adjusted via product edit. " +
                            "Old Qty: " + oldQty +
                            ", New Qty: " + newQty);

                    txnService.save(txn);
                }

                return ResponseEntity.ok(savedProduct);
            })
            .orElse(ResponseEntity.notFound().build());
}



    @GetMapping("/products/{id}/summary")
    public ResponseEntity<?> getProductSummary(
            @PathVariable Integer id) {

        Account account = SecurityUtil.getCurrentAccountId();

        return productService.getById(id, account.getId())
                .map(product -> {
                    int currentStock = product.getQuantity() == null ? 0 : product.getQuantity();

                    return ResponseEntity.ok(Map.of("currentStock", currentStock));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    

    @PostMapping("/products/{id}/add-stock")
    public ResponseEntity<?> addStock(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> body) {

        Account account = SecurityUtil.getCurrentAccountId();

        return productService.getById(id, account.getId())
                .map(product -> {
                    // Safely extract and parse quantity
                    Integer qty = Integer.parseInt(String.valueOf(body.getOrDefault("quantity", 0)));

                    String referenceNo = String.valueOf(body.getOrDefault("referenceNo", ""));
                    String notes = String.valueOf(body.getOrDefault("notes", ""));

                    // Calculate stock
                    int currentQty = product.getQuantity() != null ? product.getQuantity() : 0;
                    int newQty = currentQty + qty;

                    // Update Product
                    product.setQuantity(newQty);
                    productService.save(product);

                    // Create Transaction
                    InventoryTransaction txn = new InventoryTransaction();
                    txn.setAccountId(account.getId());
                    txn.setProductId(product.getId());
                    txn.setType("STOCK_IN");
                    txn.setQuantity(qty);
                    txn.setBalanceAfter(newQty);
                    txn.setReferenceNo(referenceNo);
                    txn.setNotes(notes);
                    txnService.save(txn);

                    return ResponseEntity.ok(Map.of(
                            "message", "Stock Added",
                            "newQuantity", newQty));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/products/{id}/transactions")
    public ResponseEntity<?> getTransactions(
            @PathVariable Integer id) {

        Account account = SecurityUtil.getCurrentAccountId();

        return productService.getById(id, account.getId())
                .map(product -> ResponseEntity.ok(
                        txnService.getByProduct(id)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Integer id) {
        Account account = SecurityUtil.getCurrentAccountId();
        return productService.getById(id, account.getId())
                .map(existing -> {
                    productService.delete(id);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** Toggle product active ↔ inactive */
    @PatchMapping("/products/{id}/toggle-status")
    public ResponseEntity<CatalogProduct> toggleProductStatus(@PathVariable Integer id) {
        Account account = SecurityUtil.getCurrentAccountId();
        return productService.toggleStatus(id, account.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/products/{id}/image")
    public ResponseEntity<?> updateProductImage(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {

        Account account = SecurityUtil.getCurrentAccountId();

        return productService.getById(id, account.getId())
                .map(p -> {
                    p.setImageUrl(body.get("imageUrl"));
                    return ResponseEntity.ok(productService.save(p));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    // =====================================================================
    // TEMPLATES
    // =====================================================================

    @GetMapping("/templates")
    public Object getTemplates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String templateType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String category) {
        Account account = SecurityUtil.getCurrentAccountId();
        Integer accountId = account.getId();

        if (templateService.countByAccountId(accountId) == 0) {
            templateService.createDefaultTemplates(accountId);
        }

        if (module != null && !module.trim().isEmpty()) {
            List<CatalogTemplate> templatesList;
            if (category != null && !category.trim().isEmpty()) {
                templatesList = templateService.getByModuleCodeAndCategoryAndAccountId(module, category, accountId);
            } else {
                templatesList = templateService.getByModuleCodeAndAccountId(module, accountId);
            }
            return Map.of("success", true, "templates", templatesList);
        }

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return templateService.getTemplatesPaginated(accountId, search, templateType, status, channel, pageable);
    }

    @PostMapping("/templates")
    public CatalogTemplate createTemplate(@RequestBody CatalogTemplate template) {
        Account account = SecurityUtil.getCurrentAccountId();
        template.setAccountId(account.getId());
        System.out.println("DTO Purpose = " + template.getPurpose());
        System.out.println("Entity Purpose Before Save = " + template.getPurpose());
        return templateService.save(template);
    }

    @PutMapping("/templates/{id}")
    public ResponseEntity<CatalogTemplate> updateTemplate(@PathVariable Integer id,
            @RequestBody CatalogTemplate template) {
        Account account = SecurityUtil.getCurrentAccountId();
        return templateService.getById(id, account.getId())
                .map(existing -> {
                    template.setId(id);
                    template.setAccountId(account.getId());
                    System.out.println("DTO Purpose = " + template.getPurpose());
                    System.out.println("Entity Purpose Before Save = " + template.getPurpose());
                    return ResponseEntity.ok(templateService.save(template));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/templates/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Integer id) {
        Account account = SecurityUtil.getCurrentAccountId();
        return templateService.getById(id, account.getId())
                .map(existing -> {
                    templateService.delete(id);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** Toggle template active ↔ inactive */
    @PatchMapping("/templates/{id}/toggle-status")
    public ResponseEntity<CatalogTemplate> toggleTemplateStatus(@PathVariable Integer id) {
        Account account = SecurityUtil.getCurrentAccountId();
        return templateService.toggleStatus(id, account.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // =====================================================================
    // REPORT TEMPLATES
    // =====================================================================

    // GET all
    @GetMapping("/rtemplates")
    public List<CatalogRTemplate> getRTemplates() {
        Account account = SecurityUtil.getCurrentAccountId();
        return rTemplateService.getByAccountId(account.getId());
    }

    // CREATE
    @PostMapping("/rtemplates")
    public CatalogRTemplate createRTemplate(@RequestBody CatalogRTemplate template) {
        Account account = SecurityUtil.getCurrentAccountId();
        template.setAccountId(account.getId());
        
        System.out.println("[TEMPLATE SAVE PAYLOAD] Title: " + template.getTitle()
                + ", Category: " + template.getCategory()
                + ", Description: " + template.getDescription()
                + ", Columns: " + template.getColumns());

        CatalogRTemplate saved = rTemplateService.save(template);

        System.out.println("[STORED COLUMNS JSON] ID: " + saved.getId()
                + ", Columns: " + saved.getColumns());

        return saved;
    }

    // UPDATE
    @PutMapping("/rtemplates/{id}")
    public ResponseEntity<CatalogRTemplate> updateRTemplate(
            @PathVariable Integer id,
            @RequestBody CatalogRTemplate template) {

        Account account = SecurityUtil.getCurrentAccountId();

        return rTemplateService.getById(id, account.getId())
                .map(existing -> {
                    template.setId(id);
                    template.setAccountId(account.getId());
                    
                    System.out.println("[TEMPLATE SAVE PAYLOAD] ID: " + id
                            + ", Title: " + template.getTitle()
                            + ", Category: " + template.getCategory()
                            + ", Description: " + template.getDescription()
                            + ", Columns: " + template.getColumns());

                    CatalogRTemplate saved = rTemplateService.save(template);

                    System.out.println("[STORED COLUMNS JSON] ID: " + saved.getId()
                            + ", Columns: " + saved.getColumns());

                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/rtemplates/{id}")
    public ResponseEntity<?> getRTemplateById(@PathVariable Integer id) {

        Account account = SecurityUtil.getCurrentAccountId();

        return rTemplateService.getById(id, account.getId())
                .map(template -> {

                    Map<String, Object> response = Map.of(
                            "id", template.getId(),
                            "title", template.getTitle(),
                            "columns", template.getParsedColumns());

                    System.out.println("[TEMPLATE LOAD RESPONSE] ID: " + id
                            + ", Response: " + response);

                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE
    @DeleteMapping("/rtemplates/{id}")
    public ResponseEntity<Void> deleteRTemplate(@PathVariable Integer id) {
        Account account = SecurityUtil.getCurrentAccountId();

        return rTemplateService.getById(id, account.getId())
                .map(existing -> {
                    rTemplateService.delete(id);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // TOGGLE STATUS
    @PatchMapping("/rtemplates/{id}/toggle-status")
    public ResponseEntity<CatalogRTemplate> toggleRTemplateStatus(@PathVariable Integer id) {
        Account account = SecurityUtil.getCurrentAccountId();

        return rTemplateService.toggleStatus(id, account.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // =====================================================================
    // SUMMARY — badge counts for the page header
    // =====================================================================

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Long>> getSummary() {
        Account account = SecurityUtil.getCurrentAccountId();
        Integer accId = account.getId();
        return ResponseEntity.ok(Map.of(
                "activePlans", planService.countActiveByAccountId(accId),
                "activeProducts", productService.countActiveByAccountId(accId),
                "activeTemplates", templateService.countActiveByAccountId(accId)));
    }

}
