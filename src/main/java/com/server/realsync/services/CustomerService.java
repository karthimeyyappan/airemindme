package com.server.realsync.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.server.realsync.entity.Customer;
import com.server.realsync.repo.CustomerRepository;

import jakarta.transaction.Transactional;

/**
 * 
 */

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer save(Customer customer) {
        return customerRepository.save(customer);
    }

    public Page<Customer> getByAccountAndGroup(Integer accountId, Integer groupId, Pageable pageable) {
        return customerRepository.findByAccountIdAndCustomerGroupId(accountId, groupId, pageable);
    }

    public Optional<Customer> getById(Integer id) {
        return customerRepository.findById(id);
    }

    public Optional<Customer> getById(Integer accountId, Integer id) {
        return customerRepository.findByIdAndAccountId(id, accountId);
    }

    public Optional<Customer> findByMobile(Integer accountId, String mobile) {
        return customerRepository.findByAccountIdAndMobile(accountId, mobile);
    }

    public Page<Customer> getByAccount(Integer accountId, Pageable pageable) {
        return customerRepository.findByAccountId(accountId, pageable);
    }

    public long getTotalCustomers(Integer accountId) {
        return customerRepository.countByAccountId(accountId);
    }

    public long getCustomersByGroup(Integer accountId, Integer groupId) {
        return customerRepository.countByAccountIdAndCustomerGroupId(accountId, groupId);
    }

    public Page<Customer> searchByAccount(Integer accountId, String search, Pageable pageable) {
        return customerRepository.searchByAccount(accountId, search, pageable);
    }

    public List<Customer> getByAccountId(Integer accountId) {
        return customerRepository.findByAccountId(accountId);
    }

    public long countByGroupId(String groupId) {
        return customerRepository.countByCustomerGroupId(groupId);
    }

    public Page<Customer> searchByAccountAndGroup(Integer accountId, Integer groupId, String search,
            Pageable pageable) {
        return customerRepository.searchByAccountAndGroup(accountId, groupId, search, pageable);
    }

    public void delete(Integer id) {
        customerRepository.deleteById(id);
    }

    public boolean isCustomerLimitReached(Integer accountId, Integer customerLimit) {
        if (customerLimit == null)
            return false; // null = unlimited (Gold/Enterprise)
        long currentCount = customerRepository.countByAccountId(accountId);
        return currentCount >= customerLimit;
    }

    // delete the cutomer grouping
    @Transactional
    public void cleanupDeletedGroup(Integer groupId) {
        // 1. Remove the ID from the comma-separated VARCHAR strings
        customerRepository.removeGroupIdFromAllCustomers(groupId);

        // 2. Clean up any leftover empty strings by setting them to NULL
        customerRepository.setEmptyGroupsToNull();
    }

}