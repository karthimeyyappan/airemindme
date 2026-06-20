package com.server.realsync.mvc.controllers;

import java.util.Optional;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.server.realsync.entity.Account;
import com.server.realsync.entity.AdminUser;
import com.server.realsync.entity.Appointment;
import com.server.realsync.entity.Customer;
import com.server.realsync.entity.CatalogPlan;
import com.server.realsync.entity.CatalogProduct;
import com.server.realsync.entity.CatalogRTemplate;
import com.server.realsync.entity.CustomerGroup;
import com.server.realsync.entity.Greeting;
import com.server.realsync.entity.InventoryTransaction;
import com.server.realsync.entity.Reminder;
import com.server.realsync.entity.ScheduleEntry;
import com.server.realsync.repo.*;
import com.server.realsync.dto.*;
import com.server.realsync.services.AccountService;
import com.server.realsync.services.CustomerService;
import com.server.realsync.services.ReminderService;
import com.server.realsync.services.GreetingService;
import com.server.realsync.services.InventoryTransactionService;
import com.server.realsync.services.PromotionService;
import com.server.realsync.services.CustomerGroupService;
import com.server.realsync.services.CatalogProductService;
import com.server.realsync.services.CatalogRTemplateService;
import com.server.realsync.services.ReportService;
import com.server.realsync.services.AdminUserService;
import com.server.realsync.services.AppointmentService;
import com.server.realsync.services.CatlogPlanService;

import com.server.realsync.entity.AccountPlan;
import com.server.realsync.entity.Promotion;
import com.server.realsync.entity.PromotionEntry;
import com.server.realsync.services.PromotionEntryService;
import com.server.realsync.services.UserService;
import com.server.realsync.util.CustomerMessageService;

import com.server.realsync.util.GmailSender;
import com.server.realsync.util.SecurityUtil;

import com.server.realsync.entity.Invoice;
import com.server.realsync.entity.InvoicePayment;
import com.server.realsync.entity.InvoiceStatus;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;

import io.swagger.v3.oas.annotations.tags.Tag;

@Controller
@RequestMapping("/")
@Tag(name = "Home API", description = "HomeController APIs")

public class HomeController {

	@Autowired
	private UserService userService;

	@Autowired
	private AccountService accountService;
	@Autowired
	CustomerMessageService customerMessageService;
	@Autowired
	private CustomerService customerService;

	@Autowired
	CustomerGroupService customerGroupService;
	@Autowired
	private CatlogPlanService settingsPlanService;
	@Autowired
	private CatalogProductService catalogProductService;
	@Autowired
	private CatalogRTemplateService catalogRTemplateService;
	@Autowired
	private AdminUserService adminUserService;
	@Autowired
	private AppointmentService appointmentService;
	@Autowired
	private ReportService reportService;
	@Autowired
	private com.server.realsync.services.AccountPlanService accountPlanService;
	@Autowired
	private ReminderService reminderService;
	@Autowired
	private GreetingService greetingService;
	@Autowired
	private PromotionService promotionService;
	@Autowired
	private PromotionEntryService promotionEntryService;
	@Autowired
	GmailSender gmailSender;
	@Autowired
	private ScheduleEntryRepository scheduleEntryRepository;
	@Autowired
	private ReminderRepository reminderRepository;
	@Autowired
	private GreetingRepository greetingRepository;
	@Autowired
	private PromotionRepository promotionRepository;
	@Autowired
	private AppointmentRepository appointmentRepository;
	@Autowired
	private InventoryTransactionService txnService;
	@Autowired
	private InvoiceRepository invoiceRepository;
	@Autowired
	private InvoicePaymentRepository invoicePaymentRepository;

	@GetMapping
	public String getWebHomePage(Model model) {
		Account account = SecurityUtil.getCurrentAccountId();
		model.addAttribute("accountId", account.getId());
		return "remindme/index.html";
	}

	@GetMapping({ "/login.html", "/signin.html" })
	public String getLogin(@RequestParam(value = "error", required = false) String error, Model model) {
		if ("true".equals(error)) {
			model.addAttribute("errorMessage", "Invalid username or password");
		}
		return "remindmeui/login";
		// return "realsync/index";
	}

	@GetMapping({ "/forgot-password.html" })
	public String getForgotPassword() {
		return "remindmeui/forgot-password";
	}

	@GetMapping({ "/signup.html", "/register.html" })
	public String getRegister(
			@RequestParam(value = "refAccId", required = false) String refAccId,
			Model model) {

		model.addAttribute("refAccId", refAccId);

		return "remindmeui/register";
	}

	@GetMapping("/home.html")
	public String getAdminDashboard(Model model) {
		Account loggedIn = SecurityUtil.getCurrentAccountId();
		Account account = accountService.getById(loggedIn.getId());

		List<Reminder> upcoming = reminderService.getTop3UpcomingByAccountId(account.getId());
		List<Integer> customerIds = upcoming.stream()
				.map(Reminder::getCustomerId)
				.filter(java.util.Objects::nonNull)
				.collect(Collectors.toList());
		Map<Integer, String> customerMap = new HashMap<>();
		if (!customerIds.isEmpty()) {
			customerService.getByAccountId(account.getId()).stream()
					.filter(c -> customerIds.contains(c.getId()))
					.forEach(c -> customerMap.put(c.getId(), c.getName()));
		}
		List<UpcomingReminderDTO> upcomingReminders = upcoming.stream().map(r -> {
			UpcomingReminderDTO dto = new UpcomingReminderDTO();
			dto.setId(r.getId());
			dto.setTitle(r.getTitle());
			dto.setChannel(r.getChannel());
			dto.setReminderDate(
					r.getReminderDate() != null ? r.getReminderDate().format(DateTimeFormatter.ofPattern("dd MMM"))
							: "");
			dto.setReminderTime(
					r.getReminderTime() != null ? r.getReminderTime().format(DateTimeFormatter.ofPattern("hh:mm a"))
							: "");
			dto.setCustomerName(customerMap.getOrDefault(r.getCustomerId(), "N/A"));
			return dto;
		}).collect(Collectors.toList());

		model.addAttribute("account", account);
		model.addAttribute("upcomingReminders", upcomingReminders);
		return "remindmeui/home";
	}

	@ResponseBody
	@GetMapping("/dashboard/stats")
	public DashboardStatsDTO stats() {
		Integer accountId = SecurityUtil.getCurrentAccountId().getId();
		long customers = customerService.getTotalCustomers(accountId);
		long scheduledActivities = scheduleEntryRepository.countByAccountId(accountId);
		long promotions = promotionService.getTotalPromotions(accountId);
		long upcomingReminders = reminderService.countScheduledByAccountId(accountId);
		return new DashboardStatsDTO(customers, scheduledActivities, promotions, upcomingReminders);
	}

	@ResponseBody
	@GetMapping("/dashboard/activity")
	public List<ActivityDataDTO> activity() {
		Integer accountId = SecurityUtil.getCurrentAccountId().getId();
		Map<String, Long> dateCounts = new java.util.LinkedHashMap<>();
		for (int i = 6; i >= 0; i--) {
			dateCounts.put(LocalDate.now().minusDays(i).toString(), 0L);
		}

		LocalDateTime startDateTime = LocalDate.now().minusDays(6).atStartOfDay();
		LocalDateTime endDateTime = LocalDate.now().atTime(23, 59, 59, 999999999);

		List<Object[]> seEntries = scheduleEntryRepository.findScheduleEntriesForActivityChart(accountId, startDateTime,
				endDateTime);
		for (Object[] row : seEntries) {
			LocalDateTime occurrenceDate = (LocalDateTime) row[0];
			if (occurrenceDate != null) {
				String dateStr = occurrenceDate.toLocalDate().toString();
				if (dateCounts.containsKey(dateStr)) {
					dateCounts.put(dateStr, dateCounts.get(dateStr) + 1);
				}
			}
		}

		List<LocalDate> apptDates = appointmentService.findAppointmentDatesForActivityChart(accountId,
				LocalDate.now().minusDays(6), LocalDate.now());
		for (LocalDate date : apptDates) {
			if (date != null) {
				String dateStr = date.toString();
				if (dateCounts.containsKey(dateStr)) {
					dateCounts.put(dateStr, dateCounts.get(dateStr) + 1);
				}
			}
		}

		List<ActivityDataDTO> response = new java.util.ArrayList<>();
		for (Map.Entry<String, Long> entry : dateCounts.entrySet()) {
			response.add(new ActivityDataDTO(entry.getKey(), entry.getValue()));
		}
		return response;
	}

	@ResponseBody
	@GetMapping("/dashboard/recent-activities")
	public List<RecentActivityDTO> recentActivities() {
		Integer accountId = SecurityUtil.getCurrentAccountId().getId();
		List<RecentActivityDTO> activities = new java.util.ArrayList<>();
		Pageable limitFive = PageRequest.of(0, 5);

		List<Reminder> reminders = reminderRepository.findByAccountIdOrderByCreatedAtDesc(accountId, limitFive);
		List<Greeting> greetings = greetingRepository.findByAccountIdOrderByCreatedAtDesc(accountId, limitFive);
		List<Appointment> appointments = appointmentRepository.findByAccountIdOrderByCreatedAtDesc(accountId,
				limitFive);
		List<Promotion> promotions = promotionRepository.findByAccountIdOrderByCreatedAtDesc(accountId, limitFive);

		java.util.Set<Integer> customerIds = new java.util.HashSet<>();
		java.util.Set<Integer> groupIds = new java.util.HashSet<>();

		for (Reminder r : reminders) {
			if (r.getCustomerId() != null)
				customerIds.add(r.getCustomerId());
		}
		for (Greeting g : greetings) {
			if (g.getCustomerId() != null)
				customerIds.add(g.getCustomerId());
			if (g.getCustomerGroupId() != null)
				groupIds.add(g.getCustomerGroupId());
		}
		for (Appointment a : appointments) {
			if (a.getCustomer() != null)
				customerIds.add(a.getCustomer().getId());
		}
		for (Promotion p : promotions) {
			if (p.getCustomerGroupId() != null)
				groupIds.add(p.getCustomerGroupId());
		}

		Map<Integer, String> customerMap = new HashMap<>();
		if (!customerIds.isEmpty()) {
			customerService.getByAccountId(accountId).stream()
					.filter(c -> customerIds.contains(c.getId()))
					.forEach(c -> customerMap.put(c.getId(), c.getName()));
		}
		Map<Integer, String> groupMap = new HashMap<>();
		if (!groupIds.isEmpty()) {
			customerGroupService.getByAccountId(accountId).stream()
					.filter(g -> groupIds.contains(g.getId()))
					.forEach(g -> groupMap.put(g.getId(), g.getName()));
		}

		for (Reminder r : reminders) {
			LocalDateTime cAt = r.getCreatedAt() != null ? r.getCreatedAt() : LocalDateTime.now();
			activities.add(new RecentActivityDTO(
					"Reminder",
					r.getTitle(),
					customerMap.getOrDefault(r.getCustomerId(), "N/A"),
					r.getStatus(),
					cAt));
		}
		for (Greeting g : greetings) {
			LocalDateTime cAt = g.getCreatedAt() != null ? g.getCreatedAt() : LocalDateTime.now();
			String name = "N/A";
			if (g.getCustomerId() != null) {
				name = customerMap.getOrDefault(g.getCustomerId(), "N/A");
			} else if (g.getCustomerGroupId() != null) {
				name = "Group: " + groupMap.getOrDefault(g.getCustomerGroupId(), "N/A");
			}
			activities.add(new RecentActivityDTO(
					"Greeting",
					g.getGreetingType() + " Greeting",
					name,
					g.getStatus(),
					cAt));
		}
		for (Appointment a : appointments) {
			LocalDateTime cAt = a.getCreatedAt() != null ? a.getCreatedAt() : LocalDateTime.now();
			String custName = a.getCustomer() != null ? a.getCustomer().getName() : "N/A";
			activities.add(new RecentActivityDTO(
					"Appointment",
					a.getServiceName(),
					custName,
					a.getStatus(),
					cAt));
		}
		for (Promotion p : promotions) {
			LocalDateTime cAt = p.getCreatedAt() != null ? p.getCreatedAt() : LocalDateTime.now();
			String groupName = p.getCustomerGroupId() != null
					? groupMap.getOrDefault(p.getCustomerGroupId(), "All Customers")
					: "All Customers";
			activities.add(new RecentActivityDTO(
					"Promotion",
					p.getAiGeneratedTitle() != null ? p.getAiGeneratedTitle() : p.getDescription(),
					"Group: " + groupName,
					p.getStatus(),
					cAt));
		}

		activities.sort((a1, a2) -> a2.getCreatedAt().compareTo(a1.getCreatedAt()));

		if (activities.size() > 10) {
			return activities.subList(0, 10);
		}
		return activities;
	}

	@GetMapping("/customers.html")
	public String getCustomers(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(required = false) String segment, // Now represents the dynamic Group ID
			@RequestParam(required = false) String search,
			Model model) {

		Account loggedIn = SecurityUtil.getCurrentAccountId();

		Account account = accountService.getById(loggedIn.getId());

		model.addAttribute("activePage", "customers");

		List<CustomerGroup> accountGroups = customerGroupService.getByAccountId(account.getId());
		model.addAttribute("customerGroups", accountGroups);

		Pageable pageable = PageRequest.of(page, 6, Sort.unsorted());
		;
		long totalCustomers = customerService.getTotalCustomers(account.getId());
		Page<Customer> customers;

		Integer groupId = null;
		if (segment != null && !segment.isBlank() && !segment.equalsIgnoreCase("all")) {
			try {

				groupId = Integer.parseInt(segment);
			} catch (NumberFormatException e) {

				groupId = null;
			}
		}

		if (search != null && !search.isBlank()) {
			if (groupId != null) {

				customers = customerService.searchByAccountAndGroup(
						account.getId(), groupId, search, pageable);
			} else {

				customers = customerService.searchByAccount(
						account.getId(), search, pageable);
			}
		} else {
			if (groupId != null) {

				customers = customerService.getByAccountAndGroup(
						account.getId(), groupId, pageable);
			} else {

				customers = customerService.getByAccount(
						account.getId(), pageable);
			}
		}
		String selectedSegmentName = null;

		if (segment != null && !segment.equalsIgnoreCase("all")) {
			for (CustomerGroup g : accountGroups) {
				if (g.getId().toString().equals(segment)) {
					selectedSegmentName = g.getName();
					break;
				}
			}
		}

		model.addAttribute("account", account);
		model.addAttribute("customers", customers.getContent());
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", customers.getTotalPages());
		model.addAttribute("totalCustomers", totalCustomers);
		model.addAttribute("search", search);
		model.addAttribute("selectedSegment", segment);
		model.addAttribute("selectedSegmentName", selectedSegmentName);
		return "remindmeui/customers";
	}

	@GetMapping("/customer-detail.html")
	public String getCustomerDetail(@RequestParam Integer id, Model model) {

		Account loggedIn = SecurityUtil.getCurrentAccountId();
		Account account = accountService.getById(loggedIn.getId());

		model.addAttribute("account", account);

		Optional<Customer> customerOpt = customerService.getById(account.getId(), id);
		if (customerOpt.isEmpty()) {
			return "redirect:/customers.html";
		}

		Customer customer = customerOpt.get();
		model.addAttribute("customer", customer);

		List<CustomerGroup> groups = customerGroupService.getByAccountId(account.getId());
		List<Reminder> reminders = reminderService.getByCustomerId(id, account.getId());
		List<Greeting> greetings = greetingService.getGreetingsForCustomer(customer);

		Map<Integer, String> groupMap = groups.stream()
				.collect(Collectors.toMap(
						CustomerGroup::getId,
						CustomerGroup::getName));

		// Invoices & Payments calculations
		List<Invoice> customerInvoices = invoiceRepository.findByCustomerId(customer.getId().longValue());
		int totalInvoices = customerInvoices.size();
		BigDecimal totalInvoiceValue = BigDecimal.ZERO;
		BigDecimal totalPaid = BigDecimal.ZERO;
		BigDecimal totalOutstanding = BigDecimal.ZERO;
		long paidInvoicesCount = 0;
		long pendingInvoicesCount = 0;

		List<Integer> invoiceIds = new ArrayList<>();
		Map<Integer, Invoice> invoiceMap = new HashMap<>();

		for (Invoice inv : customerInvoices) {
			invoiceIds.add(inv.getId().intValue());
			invoiceMap.put(inv.getId().intValue(), inv);

			if (inv.getStatus() != InvoiceStatus.CANCELLED) {
				totalInvoiceValue = totalInvoiceValue
						.add(inv.getGrandTotal() != null ? inv.getGrandTotal() : BigDecimal.ZERO);
				totalPaid = totalPaid.add(inv.getPaidAmount() != null ? inv.getPaidAmount() : BigDecimal.ZERO);
				totalOutstanding = totalOutstanding
						.add(inv.getBalanceAmount() != null ? inv.getBalanceAmount() : BigDecimal.ZERO);

				if (inv.getStatus() == InvoiceStatus.PAID) {
					paidInvoicesCount++;
				} else if (inv.getStatus() != InvoiceStatus.DRAFT) {
					pendingInvoicesCount++;
				}
			}
		}

		List<InvoicePayment> allPayments = new ArrayList<>();
		if (!invoiceIds.isEmpty()) {
			allPayments = invoicePaymentRepository.findByInvoiceIdInOrderByPaymentDateDesc(invoiceIds);
		}

		List<Map<String, Object>> paymentsList = new ArrayList<>();
		List<InvoicePayment> latestPayments = allPayments.stream().limit(5).collect(Collectors.toList());
		for (InvoicePayment p : latestPayments) {
			Map<String, Object> map = new HashMap<>();
			map.put("invoiceId", p.getInvoiceId());
			map.put("amount", p.getAmount());
			map.put("paymentMode", p.getPaymentMode());
			map.put("paymentDate", p.getPaymentDate());
			Invoice inv = invoiceMap.get(p.getInvoiceId());
			if (inv != null) {
				map.put("invoiceNumber", inv.getInvoiceNumber());
				map.put("invoiceDate", inv.getInvoiceDate());
				map.put("status", inv.getStatus().name());
			} else {
				map.put("invoiceNumber", "N/A");
				map.put("invoiceDate", null);
				map.put("status", "N/A");
			}
			paymentsList.add(map);
		}

		// Sort upcoming greetings by nearest date
		LocalDate today = LocalDate.now();
		List<Greeting> upcomingGreetings = greetings.stream()
				.filter(g -> g.getGreetingDate() != null && !g.getGreetingDate().isBefore(today))
				.sorted(Comparator.comparing(Greeting::getGreetingDate))
				.limit(3)
				.collect(Collectors.toList());

		model.addAttribute("groupMap", groupMap);
		model.addAttribute("reminders", reminders);
		model.addAttribute("greetings", greetings);
		model.addAttribute("upcomingGreetings", upcomingGreetings);
		model.addAttribute("totalInvoices", totalInvoices);
		model.addAttribute("totalInvoiceValue", totalInvoiceValue);
		model.addAttribute("totalPaid", totalPaid);
		model.addAttribute("totalOutstanding", totalOutstanding);
		model.addAttribute("paidInvoicesCount", paidInvoicesCount);
		model.addAttribute("pendingInvoicesCount", pendingInvoicesCount);
		model.addAttribute("payments", paymentsList);

		return "remindmeui/customer-detail";
	}

	@GetMapping("/promotions.html")
	public String getPromotions(Model model) {
		Account loggedIn = SecurityUtil.getCurrentAccountId();

		Account account = accountService.getById(loggedIn.getId());

		model.addAttribute("account", account);
		return "remindmeui/promotions";
	}

	@GetMapping({ "/aitemplates.html", "/ai-templates.html" })
	public String getCatalogTemplates(Model model) {

		Account loggedIn = SecurityUtil.getCurrentAccountId();
		Account account = accountService.getById(loggedIn.getId());

		model.addAttribute("account", account);

		return "remindmeui/Templates";
	}

	@GetMapping("/reminder-detail.html")
	public String getReminderDetail(@RequestParam("id") Integer id, Model model) {

		Account loggedIn = SecurityUtil.getCurrentAccountId();
		Account account = accountService.getById(loggedIn.getId());

		Optional<Reminder> reminderOpt = reminderService.getById(id, account.getId());
		if (reminderOpt.isEmpty()) {
			return "redirect:/engagement.html";
		}

		Reminder reminder = reminderOpt.get();

		// ✅ Basic
		model.addAttribute("account", account);
		model.addAttribute("reminder", reminder);

		// ✅ Customer
		Optional<Customer> customer = customerService.getById(account.getId(), reminder.getCustomerId());
		model.addAttribute("customer", customer.orElse(new Customer()));

		// ✅ Prevent Thymeleaf crash
		model.addAttribute("attachedPlan", null);
		model.addAttribute("attachedProduct", null);

		// ✅ Plan / Product logic
		if (reminder.getAttachedItemId() != null && reminder.getAttachedItemType() != null) {

			if ("plan".equalsIgnoreCase(reminder.getAttachedItemType())) {
				settingsPlanService.getById(reminder.getAttachedItemId())
						.ifPresent(plan -> model.addAttribute("attachedPlan", plan));
			}

			else if ("product".equalsIgnoreCase(reminder.getAttachedItemType())) {
				catalogProductService
						.getById(reminder.getAttachedItemId(), account.getId())
						.ifPresent(product -> model.addAttribute("attachedProduct", product));
			}
		}

		// ✅ Date formatting
		if (reminder.getCreatedAt() != null) {
			model.addAttribute("createdAtFormatted",
					reminder.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
		} else {
			model.addAttribute("createdAtFormatted", "N/A");
		}

		return "remindmeui/reminder-detail";
	}

	@GetMapping("/reminder-detail-onetime.html")
	public String getReminderDetailOneTime(@RequestParam("id") Integer id, Model model) {
		Account loggedIn = SecurityUtil.getCurrentAccountId();
		Account account = accountService.getById(loggedIn.getId());

		Optional<Reminder> reminderOpt = reminderService.getById(id, account.getId());
		if (reminderOpt.isEmpty()) {
			return "redirect:/engagement.html";
		}

		Reminder reminder = reminderOpt.get();

		// 1. Always add basic attributes first
		model.addAttribute("account", account);
		model.addAttribute("reminder", reminder);

		// 2. Safely handle Customer
		Optional<Customer> customer = customerService.getById(account.getId(), reminder.getCustomerId());
		model.addAttribute("customer", customer.orElse(new Customer()));

		// Initialize both to avoid Thymeleaf crash
		model.addAttribute("attachedPlan", null);
		model.addAttribute("attachedProduct", null);

		// Then conditionally set
		if (reminder.getAttachedItemId() != null && reminder.getAttachedItemType() != null) {

			if ("plan".equalsIgnoreCase(reminder.getAttachedItemType())) {
				settingsPlanService.getById(reminder.getAttachedItemId())
						.ifPresent(plan -> model.addAttribute("attachedPlan", plan));
			}

			else if ("product".equalsIgnoreCase(reminder.getAttachedItemType())) {
				catalogProductService.getById(reminder.getAttachedItemId(), account.getId())
						.ifPresent(product -> model.addAttribute("attachedProduct", product));
			}
		}

		// 4. Safely handle Date Formatting to prevent crash if null
		if (reminder.getCreatedAt() != null) {
			model.addAttribute("createdAtFormatted",
					reminder.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
		} else {
			model.addAttribute("createdAtFormatted", "N/A");
		}

		model.addAttribute("customer", customer.orElse(new Customer()));
		List<ScheduleEntry> entries = scheduleEntryRepository.findByReminderIdOrderByOccurrenceDateAsc(
				reminder.getId().longValue());

		model.addAttribute("entries", entries);
		ScheduleEntry entry = entries.isEmpty() ? null : entries.get(0);
		model.addAttribute("entry", entry);
		return "remindmeui/reminder-detail-onetime";
	}

	@GetMapping("/greeting-detail.html")
	public String getGreetingDetail(@RequestParam("id") Integer id, Model model) {

		Account loggedIn = SecurityUtil.getCurrentAccountId();
		Account account = accountService.getById(loggedIn.getId());

		Optional<Greeting> greetingOpt = greetingService.getById(id, account.getId());
		long memberCount = customerService.countByGroupId(String.valueOf(greetingOpt.get().getCustomerGroupId()));
		model.addAttribute("memberCount", memberCount);

		if (greetingOpt.isEmpty()) {
			return "redirect:/engagement.html";
		}

		Greeting greeting = greetingOpt.get();

		Optional<Customer> customerOpt = customerService.getById(account.getId(), greeting.getCustomerId());

		model.addAttribute("account", account);
		model.addAttribute("greeting", greeting);
		model.addAttribute("customer", customerOpt.orElse(new Customer()));

		// group details
		if (greeting.getCustomerGroupId() != null) {

			Optional<CustomerGroup> groupOpt = customerGroupService.getById(
					greeting.getCustomerGroupId());

			if (groupOpt.isPresent()) {

				CustomerGroup group = groupOpt.get();

				// long memberCount = customerService.countByGroupId(
				// String.valueOf(greeting.getCustomerGroupId()));

				model.addAttribute(
						"customerGroupName",
						group.getName());

				model.addAttribute(
						"memberCount",
						memberCount);
			}
		}

		return "remindmeui/greeting-detail";
	}

	@GetMapping("/promo/{promotionId}")
	public String openPromoLanding(@PathVariable Long promotionId,
			@RequestParam(value = "entry", required = false) Long entryId, Model model) {
		com.server.realsync.entity.Promotion promo = promotionService.getById(promotionId).orElse(null);
		if (promo == null) {
			return "error";
		}
		PromotionEntry entry = null;
		if (entryId != null) {
			entry = promotionEntryService.getById(entryId).orElse(null);
		}
		if (entry == null) {
			List<PromotionEntry> entries = promotionEntryService.getByPromotion(promotionId);
			if (!entries.isEmpty()) {
				entry = entries.get(0);
			}
		}
		model.addAttribute("promo", promo);
		model.addAttribute("entry", entry);
		return "remindmeui/promo-landing";
	}

	@GetMapping("/catalog.html")
	public String getCatalog(Model model) {

		Account loggedIn = SecurityUtil.getCurrentAccountId();

		Account account = accountService.getById(loggedIn.getId());

		model.addAttribute("account", account);

		List<CatalogPlan> plans = settingsPlanService.getByAccountId(account.getId());

		model.addAttribute("plans", plans);
		model.addAttribute("totalPlans", plans.size());
		model.addAttribute("activePlans", settingsPlanService.countActiveByAccountId(account.getId()));

		return "remindmeui/catalog";
	}

	@GetMapping("/product-details/{id}")
	public String inventoryDetail(
			@PathVariable Integer id,
			Model model) {

		Integer accountId = SecurityUtil.getCurrentAccountId().getId();

		CatalogProduct product = catalogProductService.getById(id, accountId)
				.orElseThrow(() -> new RuntimeException("Product not found"));

		List<InventoryTransaction> txns = txnService.getByProduct(id);
		int totalAdded = txns.stream().filter(t -> "STOCK_IN".equals(t.getType()))
				.mapToInt(InventoryTransaction::getQuantity).sum();
		int totalSold = txns.stream().filter(t -> "SALE".equals(t.getType()))
				.mapToInt(InventoryTransaction::getQuantity).sum();

		Map<String, Object> summary = Map.of(
				"currentStock", product.getQuantity(),
				"totalAdded", totalAdded,
				"totalSold", totalSold);

		model.addAttribute("product", product);
		model.addAttribute("summary", summary);
		model.addAttribute("transactions", txns);

		return "remindmeui/productdetail";
	}

	@GetMapping("/engagement.html")
	public String engagement(Model model) {
		Account loggedIn = SecurityUtil.getCurrentAccountId();

		Account account = accountService.getById(loggedIn.getId());

		model.addAttribute("account", account);
		return "remindmeui/engagement";
	}

	@GetMapping("/user-management.html")
	public String users(Model model) {
		Account loggedIn = SecurityUtil.getCurrentAccountId();

		Account account = accountService.getById(loggedIn.getId());

		model.addAttribute("account", account);
		model.addAttribute("activePage", "users");
		return "remindmeui/user-management";
	}

	@GetMapping("/user-detail.html")
	public String userDetail(@RequestParam Integer id, Model model) {

		Integer accountId = SecurityUtil.getCurrentAccountId().getId();

		AdminUser user = adminUserService.getById(accountId, id)
				.orElseThrow(() -> new RuntimeException("User not found"));

		model.addAttribute("user", user);
		model.addAttribute("activePage", "users");

		return "remindmeui/user-detail";
	}

	@GetMapping("/appointments.html")
	public String getAppointmentsPage(Model model) {

		Account loggedIn = SecurityUtil.getCurrentAccountId();
		Account account = accountService.getById(loggedIn.getId());
		List<Customer> customers = customerService.getByAccountId(account.getId());

		Page<AdminUser> page = adminUserService.getByAccount(account.getId(), Pageable.unpaged());
		List<AdminUser> users = page.getContent();

		model.addAttribute("customers", customers);
		model.addAttribute("users", users);
		model.addAttribute("account", account);

		return "remindmeui/appointments";
	}

	@GetMapping("/appointment-detail.html")
	public String getAppointmentDetail(@RequestParam Long id, Model model) {

		Integer accountId = SecurityUtil.getCurrentAccountId().getId();

		Optional<Appointment> apptOpt = appointmentService.getById(id, accountId);

		if (apptOpt.isEmpty()) {
			return "redirect:/appointments.html";
		}

		Appointment appt = apptOpt.get();

		model.addAttribute("appointment", appt);

		model.addAttribute("customer", appt.getCustomer() != null ? appt.getCustomer() : new Customer());

		return "remindmeui/appointment-detail";
	}

	@GetMapping("/reports.html")
	public String getAdminReport(Model model) {
		Account loggedIn = SecurityUtil.getCurrentAccountId();

		Account account = accountService.getById(loggedIn.getId());

		model.addAttribute("account", account);
		return "remindme/reports";
	}

	@GetMapping("/create-report.html")
	public String createReportPage(Model model) {
		Account loggedIn = SecurityUtil.getCurrentAccountId();

		Account account = accountService.getById(loggedIn.getId());
		model.addAttribute("account", account);

		return "remindmeui/create-report";
	}

	@GetMapping("/reports/create")
	public String createReportPage() {
		return "remindmeui/create-report";
	}

	@GetMapping("/report-history")
	public String reportHistory(Model model) {
		Account loggedIn = SecurityUtil.getCurrentAccountId();

		Account account = accountService.getById(loggedIn.getId());

		model.addAttribute("account", account);
		model.addAttribute("activePage", "reports");
		return "remindmeui/report-history";
	}

	@GetMapping("/view-report.html")
	public String viewReportPage(@RequestParam Integer id, Model model) {

		// 1. Get logged-in account
		Integer accountId = SecurityUtil.getCurrentAccountId().getId();
		Account account = accountService.getById(accountId);

		// 2. Get report (DTO)
		ReportResponse report = reportService.getReportById(id);

		if (report == null) {
			return "redirect:/reports.html"; // safety fallback
		}

		// 4. Add all required data to UI
		model.addAttribute("account", account);
		model.addAttribute("report", report);

		return "remindmeui/report-detail";
	}

	@GetMapping("/settings.html")
	public String getSettings(Model model) {

		Account loggedIn = SecurityUtil.getCurrentAccountId();

		Account account = accountService.getById(loggedIn.getId());

		List<CustomerGroup> groups = customerGroupService.getByAccountId(account.getId());

		AccountPlan accountPlan = accountPlanService.getAccountPlanUsage(account.getId())
				.orElse(null);

		List<AdminUser> users = adminUserService.getByAccount(account.getId(), Pageable.unpaged())
				.getContent();

		model.addAttribute("account", account);
		model.addAttribute("groups", groups);
		model.addAttribute("accountPlan", accountPlan);
		model.addAttribute("users", users); // <-- IMPORTANT
		model.addAttribute("activePage", "settings");

		return "remindmeui/settings";
	}

	@GetMapping("index.html")
	public String getIndexPage() {

		return "remindme/index.html";
	}

}
