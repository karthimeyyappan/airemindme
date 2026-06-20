/**
 * 
 */
package com.server.realsync.entity;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collections;
import java.util.Collection;

/**
 * 
 */
public class CustomUserDetails implements UserDetails {

    private User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    public Integer getAccountId() {
        return (user.getAccount() != null) ? user.getAccount().getId() : null;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public User getUser() {
        return user;
    }

    @Override
    public java.util.Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    public void setUser(User user) {
        this.user = user;
    }
}