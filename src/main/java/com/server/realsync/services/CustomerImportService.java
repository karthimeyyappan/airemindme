package com.server.realsync.services;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.realsync.dto.CustomerImportDto;
import com.server.realsync.dto.ImportSummaryDto;
import com.server.realsync.dto.ImportValidationResultDto;
import com.server.realsync.entity.Customer;
import com.server.realsync.entity.CustomerGroup;
import com.server.realsync.repo.CustomerGroupRepository;
import com.server.realsync.repo.CustomerRepository;

@Service
public class CustomerImportService {

    private final CustomerRepository customerRepository;
    private final CustomerGroupRepository customerGroupRepository;

    // Email pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");
    // GST pattern
    private static final Pattern GST_PATTERN = Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");

    public CustomerImportService(CustomerRepository customerRepository, CustomerGroupRepository customerGroupRepository) {
        this.customerRepository = customerRepository;
        this.customerGroupRepository = customerGroupRepository;
    }

    public ImportSummaryDto validateImport(Integer accountId, List<CustomerImportDto> rows) {
        ImportSummaryDto summary = new ImportSummaryDto();
        summary.setTotalRows(rows.size());

        List<ImportValidationResultDto> results = new ArrayList<>();
        Set<String> processedMobiles = new HashSet<>();

        // 1. Bulk pre-fetch groups
        List<CustomerGroup> dbGroups = customerGroupRepository.findByAccountId(accountId);
        Map<String, Integer> groupNameToIdMap = dbGroups.stream()
                .collect(Collectors.toMap(
                        g -> g.getName().toLowerCase().trim(),
                        CustomerGroup::getId,
                        (existing, replacement) -> existing
                ));

        // Get or create Default Group ID
        Integer uncategorizedGroupId = getOrCreateDefaultGroup(accountId, groupNameToIdMap);

        // 2. Bulk pre-fetch existing mobiles to avoid N+1 queries
        List<String> rawMobiles = rows.stream()
                .map(r -> cleanMobile(r.getMobile()))
                .filter(m -> !m.isEmpty())
                .collect(Collectors.toList());

        Set<String> dbMobiles = new HashSet<>();
        if (!rawMobiles.isEmpty()) {
            dbMobiles = customerRepository.findByAccountIdAndMobileIn(accountId, rawMobiles).stream()
                    .map(Customer::getMobile)
                    .collect(Collectors.toSet());
        }

        for (CustomerImportDto row : rows) {
            String name = row.getName() != null ? row.getName().trim() : "";
            String mobile = row.getMobile() != null ? row.getMobile().trim() : "";
            String email = row.getEmail() != null ? row.getEmail().trim() : "";
            String csvGroup = row.getCustomerGroup() != null ? row.getCustomerGroup().trim() : "";

            String status = "Ready";
            StringBuilder reason = new StringBuilder("Ready To Import");

            // Name Validation
            if (name.isEmpty()) {
                status = "Failed";
                reason = new StringBuilder("❌ Name is required");
            }

            // Mobile Validation (Rule 5)
            String cleanedMobile = cleanMobile(mobile);
            if (status.equals("Ready")) {
                if (cleanedMobile.isEmpty()) {
                    status = "Failed";
                    reason = new StringBuilder("❌ Mobile is required");
                } else if (!isValidMobile(cleanedMobile)) {
                    status = "Failed";
                    reason = new StringBuilder("❌ Invalid Mobile");
                    summary.setInvalidMobileCount(summary.getInvalidMobileCount() + 1);
                }
            }

            // Duplicate in CSV Validation (Rule 2)
            if (status.equals("Ready")) {
                if (processedMobiles.contains(cleanedMobile)) {
                    status = "Failed";
                    reason = new StringBuilder("❌ Duplicate In CSV");
                    summary.setDuplicateCsvCount(summary.getDuplicateCsvCount() + 1);
                } else {
                    processedMobiles.add(cleanedMobile);
                }
            }

            // Duplicate in DB Validation (Rule 1)
            if (status.equals("Ready")) {
                if (dbMobiles.contains(cleanedMobile)) {
                    status = "Failed";
                    reason = new StringBuilder("❌ Duplicate Mobile");
                    summary.setDuplicateDbCount(summary.getDuplicateDbCount() + 1);
                }
            }

            // Email Validation (Rule 6)
            if (status.equals("Ready") && !email.isEmpty()) {
                if (!EMAIL_PATTERN.matcher(email).matches()) {
                    status = "Failed";
                    reason = new StringBuilder("❌ Invalid Email");
                    summary.setInvalidEmailCount(summary.getInvalidEmailCount() + 1);
                }
            }

            // GST Validation (Rule 7)
            if (status.equals("Ready") && row.getGstNo() != null && !row.getGstNo().trim().isEmpty()) {
                String gst = row.getGstNo().trim();
                if (!GST_PATTERN.matcher(gst).matches()) {
                    status = "Failed";
                    reason = new StringBuilder("❌ Invalid GST");
                    summary.setInvalidGstCount(summary.getInvalidGstCount() + 1);
                }
            }

            // DOB Validation (Rule 8)
            if (status.equals("Ready") && row.getDob() != null && !row.getDob().trim().isEmpty()) {
                if (parseLocalDate(row.getDob()) == null) {
                    status = "Failed";
                    reason = new StringBuilder("❌ Invalid DOB");
                    summary.setInvalidDobCount(summary.getInvalidDobCount() + 1);
                }
            }

            // Anniversary Validation (Rule 9)
            if (status.equals("Ready") && row.getWeddingDate() != null && !row.getWeddingDate().trim().isEmpty()) {
                if (parseLocalDate(row.getWeddingDate()) == null) {
                    status = "Failed";
                    reason = new StringBuilder("❌ Invalid Anniversary Date");
                    summary.setInvalidAnniversaryCount(summary.getInvalidAnniversaryCount() + 1);
                }
            }

            // Customer Group Mapping (Rule 3 & 4)
            String mappedGroup = csvGroup;
            if (status.equals("Ready")) {
                if (csvGroup.isEmpty()) {
                    mappedGroup = "Default";
                    status = "Warning";
                    reason = new StringBuilder("⚠ Assigned To Default");
                    summary.setWarningCount(summary.getWarningCount() + 1);
                } else if (!groupNameToIdMap.containsKey(csvGroup.toLowerCase())) {
                    mappedGroup = "Default";
                    status = "Warning";
                    reason = new StringBuilder("⚠ Group Not Found (Assigned To Default)");
                    summary.setWarningCount(summary.getWarningCount() + 1);
                }
            }

            if (status.equals("Ready")) {
                summary.setReadyCount(summary.getReadyCount() + 1);
            }

            results.add(new ImportValidationResultDto(
                    name,
                    mobile,
                    email,
                    mappedGroup,
                    status,
                    reason.toString(),
                    row
            ));
        }

        summary.setResults(results);
        return summary;
    }

    @Transactional
    public Map<String, Object> importCustomers(Integer accountId, List<CustomerImportDto> rows) {
        // Run validation first
        ImportSummaryDto validationSummary = validateImport(accountId, rows);

        // Fetch groups
        List<CustomerGroup> dbGroups = customerGroupRepository.findByAccountId(accountId);
        Map<String, Integer> groupNameToIdMap = dbGroups.stream()
                .collect(Collectors.toMap(
                        g -> g.getName().toLowerCase().trim(),
                        CustomerGroup::getId,
                        (existing, replacement) -> existing
                ));
        Integer uncategorizedGroupId = getOrCreateDefaultGroup(accountId, groupNameToIdMap);

        List<Customer> toSave = new ArrayList<>();
        int imported = 0;
        int defaultGroupAssigned = 0;

        for (ImportValidationResultDto result : validationSummary.getResults()) {
            if (result.getStatus().equals("Failed")) {
                continue;
            }

            CustomerImportDto raw = result.getRawData();
            Customer customer = new Customer();
            customer.setAccountId(accountId);
            customer.setName(raw.getName().trim());
            customer.setMobile(cleanMobile(raw.getMobile()));
            customer.setEmail(raw.getEmail() != null && !raw.getEmail().trim().isEmpty() ? raw.getEmail().trim() : null);
            customer.setCity(raw.getCity() != null ? raw.getCity().trim() : null);
            customer.setAddress(raw.getAddress() != null ? raw.getAddress().trim() : null);
            customer.setGstNo(raw.getGstNo() != null ? raw.getGstNo().trim() : null);
            customer.setCustomerField1(raw.getCustomerField1() != null ? raw.getCustomerField1().trim() : null);
            customer.setCustomerField2(raw.getCustomerField2() != null ? raw.getCustomerField2().trim() : null);
            customer.setCustomerField3(raw.getCustomerField3() != null ? raw.getCustomerField3().trim() : null);
            customer.setCustomerField4(raw.getCustomerField4() != null ? raw.getCustomerField4().trim() : null);
            customer.setCustomerField5(raw.getCustomerField5() != null ? raw.getCustomerField5().trim() : null);
            
            if (raw.getDob() != null && !raw.getDob().trim().isEmpty()) {
                customer.setDob(parseLocalDate(raw.getDob()));
            }
            if (raw.getWeddingDate() != null && !raw.getWeddingDate().trim().isEmpty()) {
                customer.setWeddingDate(parseLocalDate(raw.getWeddingDate()));
            }

            // Group mapping
            String csvGroup = raw.getCustomerGroup() != null ? raw.getCustomerGroup().trim().toLowerCase() : "";
            if (csvGroup.isEmpty() || !groupNameToIdMap.containsKey(csvGroup)) {
                customer.setCustomerGroupId(uncategorizedGroupId.toString());
                defaultGroupAssigned++;
            } else {
                customer.setCustomerGroupId(groupNameToIdMap.get(csvGroup).toString());
            }

            // WhatsApp Channel Opt In (Rule 10)
            String optIn = raw.getWhatsAppOptIn() != null ? raw.getWhatsAppOptIn().trim().toLowerCase() : "";
            if (optIn.equals("yes") || optIn.equals("y") || optIn.equals("true")) {
                customer.setChannel("1"); // 1 = WhatsApp Channel
            } else {
                customer.setChannel("2"); // Default to 2 = SMS Channel
            }

            toSave.add(customer);
            imported++;
        }

        if (!toSave.isEmpty()) {
            customerRepository.saveAll(toSave);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("totalRows", rows.size());
        response.put("imported", imported);
        response.put("assignedToDefault", defaultGroupAssigned);
        response.put("skipped", rows.size() - imported);
        response.put("failed", 0); // No runtime failures since invalid/failed rows are filtered out
        return response;
    }

    private Integer getOrCreateDefaultGroup(Integer accountId, Map<String, Integer> groupNameToIdMap) {
        if (groupNameToIdMap.containsKey("uncategorized")) {
            return groupNameToIdMap.get("uncategorized");
        }
        
        Optional<CustomerGroup> existingUncat = customerGroupRepository.findByAccountIdAndNameIgnoreCase(accountId, "Default");
        if (existingUncat.isPresent()) {
            groupNameToIdMap.put("uncategorized", existingUncat.get().getId());
            return existingUncat.get().getId();
        }

        // Dynamically create Default Group
        CustomerGroup group = new CustomerGroup("Default", accountId);
        CustomerGroup saved = customerGroupRepository.save(group);
        groupNameToIdMap.put("uncategorized", saved.getId());
        return saved.getId();
    }

    private String cleanMobile(String mobile) {
        if (mobile == null) return "";
        return mobile.replaceAll("[^0-9+]", "");
    }

    private boolean isValidMobile(String cleanedMobile) {
        if (cleanedMobile.startsWith("+91")) {
            return cleanedMobile.length() == 13;
        } else if (cleanedMobile.startsWith("91") && cleanedMobile.length() == 12) {
            return true;
        } else {
            // Check if it is a standard 10 digit Indian number without prefix
            return cleanedMobile.length() == 10 && cleanedMobile.matches("\\d{10}");
        }
    }

    private LocalDate parseLocalDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        dateStr = dateStr.trim();
        List<String> formats = Arrays.asList("dd-MM-yyyy", "yyyy-MM-dd", "d-M-yyyy", "yyyy-M-d");
        for (String format : formats) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(format));
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }
}
