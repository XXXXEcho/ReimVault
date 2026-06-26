package com.company.reimbursement.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    java.util.List<User> findTop20ByRoleAndEnabledTrueAndDisplayNameContainingIgnoreCaseOrderByDisplayNameAsc(UserRole role, String keyword);

    java.util.List<User> findTop20ByRoleAndEnabledTrueOrderByDisplayNameAsc(UserRole role);
}
