package com.server.realsync.config;

public class CustomerLimitExceededException extends RuntimeException {
    public CustomerLimitExceededException(String message) {
        super(message);
    }
}