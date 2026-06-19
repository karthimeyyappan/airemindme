package com.server.realsync.mvc.controllers;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.server.realsync.entity.AdminUser;
import com.server.realsync.services.AdminUserService;
import com.server.realsync.util.SecurityUtil;

@RestController
@RequestMapping("/admin-users")
public class AdminUserController {

    private final AdminUserService service;

    public AdminUserController(AdminUserService service) {
        this.service = service;
    }

    @GetMapping
    public Page<AdminUser> getAll(Pageable pageable) {
        Integer accountId = SecurityUtil.getCurrentAccountId().getId();
        return service.getByAccount(accountId, pageable);
    }

    @PostMapping
    public AdminUser create(@RequestBody AdminUser user) {
        Integer accountId = SecurityUtil.getCurrentAccountId().getId();
        user.setAccountId(accountId);
        return service.save(user);
    }

    @PutMapping("/{id}")
    public AdminUser update(@PathVariable Integer id, @RequestBody AdminUser user) {

        Integer accountId = SecurityUtil.getCurrentAccountId().getId();

        AdminUser existing = service.getById(accountId, id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        existing.setFirstName(user.getFirstName());
        existing.setLastName(user.getLastName());
        existing.setEmail(user.getEmail());
        existing.setPhone(user.getPhone());
        existing.setRole(user.getRole());
        existing.setStatus(user.getStatus());
        existing.setPermissions(user.getPermissions());

        return service.save(existing);
    }

    @GetMapping("/{id}")
    public AdminUser getById(@PathVariable Integer id) {

        Integer accountId = SecurityUtil.getCurrentAccountId().getId();

        return service.getById(accountId, id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Integer id) {

        Integer accountId = SecurityUtil.getCurrentAccountId().getId();

        AdminUser user = service.getById(accountId, id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        service.delete(user.getId());

        return "Deleted successfully";
    }

    @GetMapping("/stats")
    public Map<String, Long> stats() {
        Integer accountId = SecurityUtil.getCurrentAccountId().getId();

        return Map.of(
                "total", service.totalUsers(accountId),
                "admins", service.totalAdmins(accountId),
                "active", service.activeUsers(accountId),
                "inactive", service.inactiveUsers(accountId));
    }
}
