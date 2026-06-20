package com.server.realsync.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.realsync.dto.CustomerFieldSettingsRequest;
import com.server.realsync.dto.CustomerFieldSettingsResponse;
import com.server.realsync.entity.Account;
import com.server.realsync.util.SecurityUtil;

@Service
public class SettingsService {

    @Autowired
    private AccountService accountService;

    public CustomerFieldSettingsResponse getCustomerFieldSettings() {
        Integer accountId = SecurityUtil.getCurrentAccountId().getId();
        Account account = accountService.getById(accountId);

        CustomerFieldSettingsResponse response = new CustomerFieldSettingsResponse();
        response.setField1Name(account.getCustomerField1Name() != null ? account.getCustomerField1Name() : "");
        response.setField2Name(account.getCustomerField2Name() != null ? account.getCustomerField2Name() : "");
        response.setField3Name(account.getCustomerField3Name() != null ? account.getCustomerField3Name() : "");
        response.setField4Name(account.getCustomerField4Name() != null ? account.getCustomerField4Name() : "");
        response.setField5Name(account.getCustomerField5Name() != null ? account.getCustomerField5Name() : "");
        return response;
    }

    @Transactional
    public CustomerFieldSettingsResponse updateCustomerFieldSettings(CustomerFieldSettingsRequest request) {
        Integer accountId = SecurityUtil.getCurrentAccountId().getId();
        Account account = accountService.getById(accountId);

        account.setCustomerField1Name(request.getField1Name() != null ? request.getField1Name() : "");
        account.setCustomerField2Name(request.getField2Name() != null ? request.getField2Name() : "");
        account.setCustomerField3Name(request.getField3Name() != null ? request.getField3Name() : "");
        account.setCustomerField4Name(request.getField4Name() != null ? request.getField4Name() : "");
        account.setCustomerField5Name(request.getField5Name() != null ? request.getField5Name() : "");

        accountService.save(account);

        CustomerFieldSettingsResponse response = new CustomerFieldSettingsResponse();
        response.setField1Name(account.getCustomerField1Name());
        response.setField2Name(account.getCustomerField2Name());
        response.setField3Name(account.getCustomerField3Name());
        response.setField4Name(account.getCustomerField4Name());
        response.setField5Name(account.getCustomerField5Name());
        return response;
    }
}
