package com.server.realsync.mvc.controllers;

import java.util.Optional;
import java.util.Map;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.server.realsync.entity.Account;
import com.server.realsync.entity.AccountPlan;
import com.server.realsync.entity.Customer;
import com.server.realsync.repo.AccountPlanRepository;
import com.server.realsync.services.CustomerService;
import com.server.realsync.services.AccountService;
import com.server.realsync.util.SecurityUtil;
import com.server.realsync.config.CustomerLimitExceededException;

@RestController
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountPlanRepository accountPlanRepository;

    // ===============================
    // LIMIT STATUS API
    // ===============================
    @GetMapping("/api/customers/limit-status")
    public ResponseEntity<?> getLimitStatus() {
        Account account = SecurityUtil.getCurrentAccountId();
        long currentCount = customerService.getTotalCustomers(account.getId());
        
        Optional<AccountPlan> accountPlan = accountPlanRepository.findByAccountIdAndStatus(
                account.getId(), AccountPlan.PlanStatus.active);
        
        if (accountPlan.isEmpty()) {
            return ResponseEntity.ok(Map.of("limitReached", false, "current", currentCount, "limit", -1));
        }
        
        Integer limit = accountPlan.get().getPlan().getCustomerLimit();
        boolean limitReached = limit != null && currentCount >= limit;
        
        return ResponseEntity.ok(Map.of(
            "limitReached", limitReached,
            "current", currentCount,
            "limit", limit != null ? limit : -1
        ));
    }

    // ===============================
    // CREATE CUSTOMER API
    // ===============================
    @PostMapping("/api/customers")
    @ResponseBody
    public ResponseEntity<?> createCustomer(@RequestBody Customer customer) {

        Account account = SecurityUtil.getCurrentAccountId();
        customer.setAccountId(account.getId());

        // Check customer limit
        Optional<AccountPlan> accountPlan = accountPlanRepository.findByAccountIdAndStatus(
                account.getId(),
                AccountPlan.PlanStatus.active);

        if (accountPlan.isPresent()) {

            Integer limit = accountPlan.get().getPlan().getCustomerLimit();

            if (limit != null) {

                long count = customerService.getTotalCustomers(account.getId());

                if (count >= limit) {
                    throw new CustomerLimitExceededException(
                            "Customer limit reached (" + limit + "). Please upgrade your plan.");
                }
            }
        }

        Optional<Customer> existing = customerService.findByMobile(account.getId(), customer.getMobile());
        if (existing.isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Customer with this mobile already exists"));
        }

        Customer saved = customerService.save(customer);
        return ResponseEntity.ok(saved);
    }

    // ===============================
    // UPDATE CUSTOMER API
    // ===============================

    @PutMapping("/api/customers/{id}")
    public ResponseEntity<?> updateCustomer(
            @PathVariable Integer id,
            @RequestBody Customer customer) {

        Account account = SecurityUtil.getCurrentAccountId();

        Optional<Customer> optionalCustomer = customerService.getById(id);

        if (optionalCustomer.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message",
                            "Customer not found"));
        }

        Customer existing = optionalCustomer.get();

        if (!existing.getAccountId().equals(account.getId())) {
            return ResponseEntity.status(403)
                    .body(Map.of("message",
                            "Unauthorized"));
        }

        Optional<Customer> duplicate = customerService.findByMobile(
                account.getId(),
                customer.getMobile());

        if (duplicate.isPresent()
                && !duplicate.get().getId().equals(id)) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Customer with this mobile already exists"));
        }

        existing.setName(customer.getName());
        existing.setMobile(customer.getMobile());
        existing.setEmail(customer.getEmail());
        existing.setDob(customer.getDob());
        existing.setWeddingDate(customer.getWeddingDate());
        existing.setChannel(customer.getChannel());
        existing.setCustomerGroupId(customer.getCustomerGroupId());
        existing.setGstNo(customer.getGstNo());
        existing.setAddress(customer.getAddress());
        existing.setCity(customer.getCity());
        existing.setState(customer.getState());
        existing.setCountry(customer.getCountry());
        existing.setCustomerField1(customer.getCustomerField1());
        existing.setCustomerField2(customer.getCustomerField2());
        existing.setCustomerField3(customer.getCustomerField3());
        existing.setCustomerField4(customer.getCustomerField4());
        existing.setCustomerField5(customer.getCustomerField5());

        Customer saved = customerService.save(existing);

        return ResponseEntity.ok(saved);
    }

    @GetMapping("/api/customers/search")
    public ResponseEntity<List<Customer>> searchCustomers(
            @RequestParam String query) {

        Account account = SecurityUtil.getCurrentAccountId();

        String cleanQuery = query.trim();
        if (cleanQuery.toUpperCase().startsWith("CUS-")) {
            cleanQuery = cleanQuery.substring(4).trim();
        }

        Page<Customer> page = customerService.searchByAccount(
                account.getId(),
                cleanQuery,
                Pageable.ofSize(20));

        return ResponseEntity.ok(page.getContent());
    }

    @GetMapping("/api/customers/my-customers")
    public ResponseEntity<?> getMyCustomers() {

        Account account = SecurityUtil.getCurrentAccountId();

        if (account == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        List<Customer> customers = customerService.getByAccountId(account.getId());

        return ResponseEntity.ok(customers);
    }

    @GetMapping("/api/customers/{id}")
    public ResponseEntity<?> getCustomerById(@PathVariable Integer id) {

        Account account = SecurityUtil.getCurrentAccountId();

        Optional<Customer> customer = customerService.getById(id);

        if (customer.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // SECURITY CHECK (IMPORTANT)
        if (!customer.get().getAccountId().equals(account.getId())) {
            return ResponseEntity.status(403).body("Unauthorized");
        }

        return ResponseEntity.ok(customer.get());
    }

    @GetMapping("/api/customers/template")
    public ResponseEntity<Resource> downloadTemplate() {
        Account account = SecurityUtil.getCurrentAccountId();
        Account fullAccount = accountService.getById(account.getId());

        StringBuilder headers = new StringBuilder(
                "name,phone,email,segment,city,address,birthday,anniversary,gstNo,whatsAppOptIn");
        StringBuilder sample = new StringBuilder(
                "Rajesh Kumar,+919876543210,rajesh@gmail.com,VIP,Chennai,123 Main St,1995-06-15,2020-01-10,22AAAAA1111A1Z1,Yes");

        if (fullAccount.getCustomerField1Name() != null && !fullAccount.getCustomerField1Name().trim().isEmpty()) {
            headers.append(",").append(fullAccount.getCustomerField1Name().trim());
            sample.append(",Value1");
        } else {
            headers.append(",Field1");
            sample.append(",");
        }
        if (fullAccount.getCustomerField2Name() != null && !fullAccount.getCustomerField2Name().trim().isEmpty()) {
            headers.append(",").append(fullAccount.getCustomerField2Name().trim());
            sample.append(",Value2");
        } else {
            headers.append(",Field2");
            sample.append(",");
        }
        if (fullAccount.getCustomerField3Name() != null && !fullAccount.getCustomerField3Name().trim().isEmpty()) {
            headers.append(",").append(fullAccount.getCustomerField3Name().trim());
            sample.append(",Value3");
        } else {
            headers.append(",Field3");
            sample.append(",");
        }
        if (fullAccount.getCustomerField4Name() != null && !fullAccount.getCustomerField4Name().trim().isEmpty()) {
            headers.append(",").append(fullAccount.getCustomerField4Name().trim());
            sample.append(",Value4");
        } else {
            headers.append(",Field4");
            sample.append(",");
        }
        if (fullAccount.getCustomerField5Name() != null && !fullAccount.getCustomerField5Name().trim().isEmpty()) {
            headers.append(",").append(fullAccount.getCustomerField5Name().trim());
            sample.append(",Value5");
        } else {
            headers.append(",Field5");
            sample.append(",");
        }

        headers.append("\n");
        sample.append("\n");

        String csv = headers.toString() + sample.toString();
        ByteArrayResource resource = new ByteArrayResource(csv.getBytes(StandardCharsets.UTF_8));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=customer_import_template.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }

}