package com.company.reimbursement.auth;

import com.company.reimbursement.user.User;
import com.company.reimbursement.user.UserRole;

public record MeResponse(Long id, String username, String displayName, String department, UserRole role) {
    public static MeResponse from(User user) {
        return new MeResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getDepartment(), user.getRole());
    }
}
