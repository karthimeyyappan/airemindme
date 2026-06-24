package com.server.realsync.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.server.realsync.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByResetToken(String resetToken);
    
    List<User> findByAccountId(Integer accountId);
    
    // 🔹 Count users by Role ID
    long countByRoleId(Long roleId);

    // (Optional) Count users by Account ID
    long countByAccountId(Integer accountId);
    
    List<User> findByAccountIdAndRoleId(Integer accountId, Long roleId);
    
    Optional<User> findByUsernameAndAccountId(String username, Integer accountId);
    
}

