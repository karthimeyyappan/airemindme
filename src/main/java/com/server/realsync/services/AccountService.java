package com.server.realsync.services;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.realsync.config.AccountNotFoundException;
import com.server.realsync.dto.SignupRequestDto;
import com.server.realsync.entity.Account;
import com.server.realsync.entity.Role;
import com.server.realsync.entity.User;
import com.server.realsync.entity.AccountPlan;
import com.server.realsync.entity.Plan;
import com.server.realsync.repo.AccountRepository;
import com.server.realsync.repo.RoleRepository;
import com.server.realsync.repo.UserRepository;
import com.server.realsync.repo.AccountPlanRepository;
import com.server.realsync.repo.PlanRepository;
import com.server.realsync.repo.CreditTransactionRepository;
import com.server.realsync.entity.CreditTransaction;

@Service
public class AccountService {

    @Autowired
    private AccountRepository repository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccountPlanRepository accountPlanRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private CreditTransactionRepository creditTransactionRepository;

    public List<Account> findAll() {
        return repository.findAll();
    }

    public Optional<Account> findById(Integer id) {
        return repository.findById(id);
    }

    public Account save(Account account) {
        return repository.save(account);
    }

    public Account getById(int id) {
        return repository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
    }

    public void deleteById(Integer id) {
        repository.deleteById(id);
    }

    public boolean emailExists(String email) {
        return repository.findByEmail(email).isPresent();
    }

    public boolean mobileExists(String mobile) {
        return userRepository.findByUsername(mobile).isPresent();
    }

    @Transactional
    public void registerAccount(SignupRequestDto dto) {
        // Validate inputs
        validateSignupRequest(dto);

        // Duplicate checks
        if (repository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalStateException("Email already registered");
        }
        if (userRepository.findByUsername(dto.getMobile()).isPresent()) {
            throw new IllegalStateException("Mobile number already registered");
        }

        // Create Account
        Account account = new Account();
        account.setName(dto.getName());
        account.setEmail(dto.getEmail());
        account.setMobile(dto.getMobile());
        account.setBusinessName(dto.getBusinessName());
        account.setBusinessEmail(dto.getBusinessEmail());
        account.setBusinessPhone(dto.getBusinessPhone());
        account.setGstNumber(dto.getGstNumber());
        account.setAddress(dto.getAddress());
        account.setCategory(dto.getBusinessCategory());
        account.setCountry(dto.getCountry());
        account.setCurrency(dto.getCurrency());
        account.setLanguage(dto.getDefaultLanguage());
        Account savedAccount = repository.save(account);

        // Fetch/Create ROLE_USER Role
        Role userRole = roleRepository.findByName("ROLE_USER").orElseGet(() -> {
            Role newRole = new Role();
            newRole.setName("ROLE_USER");
            return roleRepository.save(newRole);
        });

        // Create User
        User user = new User();
        user.setUsername(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setFullName(dto.getName());
        user.setMobile(dto.getMobile());
        user.setAccount(savedAccount);
        user.setRole(userRole);
        userRepository.save(user);

        // Create AccountPlan
        String planName = dto.getSelectedPlan() != null ? dto.getSelectedPlan() : "Starter";
        Plan plan = planRepository.findByNameIgnoreCase(planName).orElseGet(() -> {
            Plan newPlan = new Plan();
            newPlan.setName(planName);
            newPlan.setChargePerAI(0.0);
            newPlan.setChargePerNonAI(0.0);
            newPlan.setChargePerAiPhoto(0.0);
            newPlan.setChargePerNonAiPhoto(0.0);
            return planRepository.save(newPlan);
        });

        double credits = 50.0;
        if ("Growth".equalsIgnoreCase(planName)) {
            credits = 150.0;
        } else if ("Business".equalsIgnoreCase(planName)) {
            credits = 300.0;
        }

        AccountPlan accountPlan = new AccountPlan();
        accountPlan.setAccount(savedAccount);
        accountPlan.setPlan(plan);
        accountPlan.setStartDate(LocalDate.now());
        accountPlan.setEndDate(LocalDate.now().plusDays(30)); // default 30 days validity period
        accountPlan.setBalance(credits);
        accountPlan.setTotalCredits(credits);
        AccountPlan savedAccountPlan = accountPlanRepository.save(accountPlan);

        // Insert CreditTransaction
        CreditTransaction transaction = new CreditTransaction();
        transaction.setAccountId(savedAccount.getId());
        transaction.setAccountPlanId(savedAccountPlan.getId());
        transaction.setType("PLAN_BOUGHT");
        transaction.setCredits(credits);
        transaction.setBalanceAfter(credits);
        transaction.setRemarks(planName + " Plan Purchased");
        creditTransactionRepository.save(transaction);
    }

    private void validateSignupRequest(SignupRequestDto dto) {
        if (dto.getName() == null || dto.getName().trim().length() < 3) {
            throw new IllegalArgumentException("Full Name must be at least 3 characters.");
        }
        if (dto.getEmail() == null || !dto.getEmail().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            throw new IllegalArgumentException("Please provide a valid email address.");
        }
        if (dto.getMobile() == null || !dto.getMobile().matches("^\\d{10}$")) {
            throw new IllegalArgumentException("Mobile number must be exactly 10 digits.");
        }
        if (dto.getBusinessName() == null || dto.getBusinessName().trim().isEmpty()) {
            throw new IllegalArgumentException("Business name is required.");
        }
       

        String password = dto.getPassword();
        if (password == null || password.length() < 8 ||
                !password.matches(".*[A-Z].*") || !password.matches(".*[a-z].*") || !password.matches(".*\\d.*")) {
            throw new IllegalArgumentException(
                    "Password must be at least 8 characters and contain at least one uppercase letter, one lowercase letter, and one number.");
        }
    }

}