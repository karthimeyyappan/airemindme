package com.server.realsync.services;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.realsync.config.AccountNotFoundException;
import com.server.realsync.dto.SignupRequestDto;
import com.server.realsync.entity.Account;
import com.server.realsync.entity.AccountPlan;
import com.server.realsync.entity.CreditTransaction;
import com.server.realsync.entity.Plan;
import com.server.realsync.entity.Role;
import com.server.realsync.entity.User;
import com.server.realsync.repo.AccountPlanRepository;
import com.server.realsync.repo.AccountRepository;
import com.server.realsync.repo.CreditTransactionRepository;
import com.server.realsync.repo.PlanRepository;
import com.server.realsync.repo.RoleRepository;
import com.server.realsync.repo.UserRepository;

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
        validateSignupRequest(dto);

        if (repository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalStateException("Email already registered");
        }
        if (userRepository.findByUsername(dto.getMobile()).isPresent()) {
            throw new IllegalStateException("Mobile number already registered");
        }

        // 1. Create Account
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
<<<<<<< HEAD
        Account savedAccount = repository.save(account);

        // Auto-generate this account's own referral code
        savedAccount.setReferralCode("NUMEN-" + savedAccount.getId());

        // If someone referred this new account, save referred_by
        if (dto.getReferralCode() != null && !dto.getReferralCode().isEmpty()) {
            repository.findByReferralCode(dto.getReferralCode())
                    .ifPresent(referrer -> {
                        savedAccount.setReferredBy(referrer.getId());
                    });
        }

        // Save again with referral_code and referred_by
=======

        if (dto.getRefAccId() != null && !dto.getRefAccId().trim().isEmpty()) {
            try {
                String code = dto.getRefAccId().trim().toUpperCase();
                if (code.startsWith("NUMEN-")) {
                    Integer referrerId = Integer.parseInt(
                            code.replace("NUMEN-", "").trim());
                    account.setReferredBy(referrerId);
                }
            } catch (Exception e) {
                // Invalid referral code - ignore silently
            }
        }

        Account savedAccount = repository.save(account);

        // Auto-generate referral ID
        savedAccount.setReferralId("NUMEN-" + savedAccount.getId());
>>>>>>> 5ac4df241cf1277df1e653972e41b3578048dbd1
        repository.save(savedAccount);

        // 2. Assign Role
        Role userRole = roleRepository.findByName("ROLE_USER").orElseGet(() -> {
            Role newRole = new Role();
            newRole.setName("ROLE_USER");
            return roleRepository.save(newRole);
        });

        // 3. Create User
        User user = new User();
        user.setUsername(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setFullName(dto.getName());
        user.setMobile(dto.getMobile());
        user.setAccount(savedAccount);
        user.setRole(userRole);
        userRepository.save(user);

        // 4. Always assign Free Trial plan on new registration
        Plan freeTrial = planRepository.findByIsTrial(true)
                .orElseThrow(() -> new RuntimeException(
                        "Free Trial plan not found. Please insert it in the plan table."));

        AccountPlan accountPlan = new AccountPlan();
        accountPlan.setAccount(savedAccount);
        accountPlan.setPlan(freeTrial);
        accountPlan.setStartDate(LocalDate.now());
        accountPlan.setEndDate(LocalDate.now().plusDays(freeTrial.getTrialDays())); // 7 days
        accountPlan.setTotalCredits(Double.valueOf(freeTrial.getWhatsappCredits())); // 40
        accountPlan.setBalance(freeTrial.getWhatsappCredits()); // 40
        accountPlan.setStatus(AccountPlan.PlanStatus.active);
        accountPlan.setTransaction(null); // no payment for trial
        AccountPlan savedAccountPlan = accountPlanRepository.save(accountPlan);

        // 5. Log credit transaction for trial
        CreditTransaction ct = new CreditTransaction();
        ct.setAccountId(savedAccount.getId());
        ct.setAccountPlanId(savedAccountPlan.getId());
        ct.setType("TRIAL_ASSIGNED");
        ct.setCredits(Double.valueOf(freeTrial.getWhatsappCredits()));
        ct.setBalanceAfter(Double.valueOf(freeTrial.getWhatsappCredits()));
        ct.setRemarks("Free Trial assigned - " + freeTrial.getTrialDays() + " days");
        creditTransactionRepository.save(ct);
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
                !password.matches(".*[A-Z].*") ||
                !password.matches(".*[a-z].*") ||
                !password.matches(".*\\d.*")) {
            throw new IllegalArgumentException(
                    "Password must be at least 8 characters and contain at least one uppercase letter, one lowercase letter, and one number.");
        }
    }
}