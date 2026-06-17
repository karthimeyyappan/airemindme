package com.server.realsync.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.server.realsync.entity.CatalogTemplate;
import com.server.realsync.repo.CatalogTemplateRepository;

@Service
public class CatalogTemplateService {

    @Autowired
    private CatalogTemplateRepository repo;

    /** All templates for an account, newest first */
    public List<CatalogTemplate> getByAccountId(Integer accountId) {
        return repo.findByAccountIdOrderByCreatedAtDesc(accountId);
    }

    /** Single template scoped to account */
    public Optional<CatalogTemplate> getById(Integer id, Integer accountId) {
        return repo.findByIdAndAccountId(id, accountId);
    }

    /** Create or update */
    public CatalogTemplate save(CatalogTemplate template) {
        return repo.save(template);
    }

    /** Hard delete */
    public void delete(Integer id) {
        repo.deleteById(id);
    }

    /** Count active templates */
    public long countActiveByAccountId(Integer accountId) {
        return repo.countByAccountIdAndStatus(accountId, "active");
    }

    /**
     * Toggle status: active ↔ inactive
     * Returns the updated entity, or empty if not found / not owned.
     */
    public Optional<CatalogTemplate> toggleStatus(Integer id, Integer accountId) {
        return repo.findByIdAndAccountId(id, accountId).map(t -> {
            t.setStatus("active".equals(t.getStatus()) ? "inactive" : "active");
            return repo.save(t);
        });
    }

    public List<CatalogTemplate> getByModuleCodeAndAccountId(String moduleCode, Integer accountId) {
        return repo.findByModuleCodeAndAccountIdOrderByCreatedAtDesc(moduleCode, accountId);
    }

    public List<CatalogTemplate> getByModuleCodeAndCategoryAndAccountId(String moduleCode, String category, Integer accountId) {
        return repo.findByModuleCodeAndCategoryAndAccountIdOrderByCreatedAtDesc(moduleCode, category, accountId);
    }

    public org.springframework.data.domain.Page<CatalogTemplate> getTemplatesPaginated(
            Integer accountId, String search, String templateType, String status, String channel,
            org.springframework.data.domain.Pageable pageable) {
        return repo.searchAndFilterTemplates(accountId, search, templateType, status, channel, pageable);
    }

    public long countByAccountId(Integer accountId) {
        return repo.findByAccountIdOrderByCreatedAtDesc(accountId).size();
    }

    public void createDefaultTemplates(Integer accountId) {
        // Template 1: REMINDER - EMI
        CatalogTemplate t1 = new CatalogTemplate();
        t1.setAccountId(accountId);
        t1.setTitle("EMI Payment Due");
        t1.setName("EMI Payment Due");
        t1.setCategory("Invoice");
        t1.setDescription("Default template for EMI/Invoice payment reminders.");
        t1.setContent("A payment of {amount} is due on {due_date}. Kindly complete it on time to avoid any interruption in your service. If payment has already been made, please ignore this reminder.");
        t1.setTemplateType("Reminder");
        t1.setModuleCode("REMINDER");
        t1.setChannels("WhatsApp,SMS,Email");
        t1.setStatus("active");
        t1.setActive(true);
        repo.save(t1);

        // Template 2: REMINDER - Policy
        CatalogTemplate t2 = new CatalogTemplate();
        t2.setAccountId(accountId);
        t2.setTitle("Policy Renewal");
        t2.setName("Policy Renewal");
        t2.setCategory("Invoice");
        t2.setDescription("Default template for policy/subscription renewals.");
        t2.setContent("Your subscription/policy is approaching renewal. Renew before {due_date} to continue enjoying uninterrupted benefits and services.");
        t2.setTemplateType("Reminder");
        t2.setModuleCode("REMINDER");
        t2.setChannels("WhatsApp,SMS,Email");
        t2.setStatus("active");
        t2.setActive(true);
        repo.save(t2);

        // Template 3: REMINDER - Appointment
        CatalogTemplate t3 = new CatalogTemplate();
        t3.setAccountId(accountId);
        t3.setTitle("Appointment Reminder");
        t3.setName("Appointment Reminder");
        t3.setCategory("Reminder");
        t3.setDescription("Default template for appointment notifications.");
        t3.setContent("Your appointment has been successfully scheduled and is ready for review.");
        t3.setTemplateType("Reminder");
        t3.setModuleCode("REMINDER");
        t3.setChannels("WhatsApp,SMS,Email");
        t3.setStatus("active");
        t3.setActive(true);
        repo.save(t3);

        // Template 4: GREETING - Welcome
        CatalogTemplate t4 = new CatalogTemplate();
        t4.setAccountId(accountId);
        t4.setTitle("Welcome Message");
        t4.setName("Welcome Message");
        t4.setCategory("Greeting");
        t4.setDescription("Default template for welcoming new users.");
        t4.setContent("Welcome to our community! We are excited to have you on board. If you need any assistance, feel free to reach out.");
        t4.setTemplateType("Greeting");
        t4.setModuleCode("GREETING");
        t4.setChannels("WhatsApp,SMS,Email");
        t4.setStatus("active");
        t4.setActive(true);
        repo.save(t4);

        // --- default PROMOTION templates ---
        createPromoTemplate(accountId, "Diwali Offer", "festival", 
            "Festive offer for Diwali celebrations.",
            List.of("WhatsApp", "SMS", "Email"),
            List.of(
                "Hello {CustomerName} 👋 Wishing you a happy Diwali! Celebrate prosperity with our exclusive festive bundles: {PROMOTION_URL}",
                "Dear Valued Customer, on this auspicious occasion of Diwali, explore our curated investment products: {PROMOTION_URL}",
                "Celebrate prosperity. Unlock exclusive premium wealth portfolios tailored for your family this Diwali: {PROMOTION_URL}",
                "Last chance! Our exclusive Diwali special promotion is ending soon. Secure your benefits now: {PROMOTION_URL}"
            )
        );

        createPromoTemplate(accountId, "Festival Special", "festival", 
            "Festive season general promotion template.",
            List.of("WhatsApp", "SMS", "Email"),
            List.of(
                "Hello! Make this festival memorable for your family with our special credit & savings offers: {PROMOTION_URL}",
                "Dear Client, explore our corporate special financial and credit solutions for the festive season: {PROMOTION_URL}",
                "Hey! Upgrade your festive vibe with zero-interest EMI schemes on your favorite gadgets: {PROMOTION_URL}"
            )
        );

        createPromoTemplate(accountId, "New Product Launch", "product", 
            "Announce a newly launched plan or product.",
            List.of("WhatsApp", "SMS", "Email"),
            List.of(
                "Exciting news! We just launched a new high-yield savings plan. Check it out: {PROMOTION_URL}",
                "We are pleased to introduce our new asset management portfolio, designed for consistent growth: {PROMOTION_URL}",
                "Be the first to invest. Slots for our newly launched wealth fund are filling fast: {PROMOTION_URL}"
            )
        );

        createPromoTemplate(accountId, "Membership Upgrade", "vip", 
            "Offer membership upgrades to eligible customers.",
            List.of("WhatsApp", "Email"),
            List.of(
                "Hey {CustomerName}, you're ready for the next level! Check out your custom upgrade options: {PROMOTION_URL}",
                "Dear member, upgrade your plan to access premium advisory and wealth services: {PROMOTION_URL}"
            )
        );

        createPromoTemplate(accountId, "Flash Sale", "seasonal", 
            "Urgent short-term promotional sale.",
            List.of("WhatsApp", "SMS"),
            List.of(
                "Flash Sale! Get premium financial tools and zero-brokerage benefits for 24 hours only: {PROMOTION_URL}",
                "Hurry! Time is ticking. Claim your special offer before it expires tonight: {PROMOTION_URL}"
            )
        );

        createPromoTemplate(accountId, "Referral Campaign", "seasonal", 
            "Encourage users to refer others for rewards.",
            List.of("WhatsApp", "SMS", "Email"),
            List.of(
                "Invite your friends to start their investment journey. You both get cash benefits: {PROMOTION_URL}",
                "Share the smart way to invest. Refer business partners and earn referral rewards: {PROMOTION_URL}"
            )
        );

        createPromoTemplate(accountId, "Exclusive VIP Offer", "vip", 
            "High-value deals for VIP clients.",
            List.of("WhatsApp", "Email"),
            List.of(
                "Dear VIP customer, enjoy priority access to our top-performing wealth plans: {PROMOTION_URL}",
                "Unlock your executive privileges. Review your personalized VIP savings offer: {PROMOTION_URL}"
            )
        );

        createPromoTemplate(accountId, "Insurance Renewal Offer", "financial", 
            "Special renewal rates for insurance policies.",
            List.of("WhatsApp", "SMS", "Email"),
            List.of(
                "Your policy is expiring. Renew today to get 15% bonus coverage at no extra cost: {PROMOTION_URL}",
                "Avoid policy lapse. Renew your insurance plan in 2 clicks for continuous coverage: {PROMOTION_URL}"
            )
        );

        createPromoTemplate(accountId, "Financial Product Promotion", "financial", 
            "Promote banking, credit, or financial services.",
            List.of("WhatsApp", "SMS", "Email"),
            List.of(
                "Need a financial boost? Check out our pre-approved low-interest personal plans: {PROMOTION_URL}",
                "Optimize your capital structure with our customized term loan offerings: {PROMOTION_URL}"
            )
        );

        createPromoTemplate(accountId, "Investment Campaign", "financial", 
            "Encourage savings and investments.",
            List.of("WhatsApp", "Email"),
            List.of(
                "Start your SIP journey with just 500/month and watch your wealth compound: {PROMOTION_URL}",
                "Maximize your tax savings under Section 80C with our top-rated ELSS funds: {PROMOTION_URL}"
            )
        );
    }

    private void createPromoTemplate(Integer accountId, String name, String category, String desc, List<String> channels, List<String> variants) {
        CatalogTemplate t = new CatalogTemplate();
        t.setAccountId(accountId);
        t.setTitle(name);
        t.setName(name);
        t.setCategory(category);
        t.setDescription(desc);
        t.setContent(variants.isEmpty() ? "" : variants.get(0));
        t.setTemplateType("Promotion");
        t.setModuleCode("PROMOTION");
        t.setChannels(String.join(",", channels));
        t.setStatus("active");
        t.setActive(true);
        t.setVariantCount(variants.size());
        
        org.json.JSONArray arr = new org.json.JSONArray(variants);
        t.setVariantsJson(arr.toString());
        repo.save(t);
    }
}