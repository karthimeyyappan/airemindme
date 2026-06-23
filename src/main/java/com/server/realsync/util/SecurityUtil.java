/**
 * 
 */
package com.server.realsync.util;

/**
 * 
 */

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.server.realsync.entity.Account;
import com.server.realsync.entity.CustomUserDetails;
import com.server.realsync.entity.User;


public class SecurityUtil {

    /**
     * Returns the currently authenticated user's account ID.
     * Returns 0 if no authentication or invalid principal.
     */
    public static Account getCurrentAccountId() {
        Account account = new Account();
    	int accountId = 0;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
            accountId = customUserDetails.getAccountId();
        }
        account.setId(accountId);
        return account;
    }
    
    public static User getLoggedInUser() {
        User user = null;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
         System.out.println("AUTH = " + authentication);
    System.out.println("PRINCIPAL CLASS = "
            + authentication.getPrincipal().getClass());
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
            user = customUserDetails.getUser();
        }
        return user;
    }
}
