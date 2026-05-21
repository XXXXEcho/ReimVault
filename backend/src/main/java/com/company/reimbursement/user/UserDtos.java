package com.company.reimbursement.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UserDtos {
    public record CreateUserRequest(
            @NotBlank String username,
            @NotBlank String displayName,
            String department,
            @NotBlank String password,
            @NotNull UserRole role
    ) {
    }

    public record UpdateUserRequest(@NotBlank String displayName, String department, @NotNull UserRole role, boolean enabled) {
    }

    public record UserResponse(Long id, String username, String displayName, String department, UserRole role, boolean enabled) {
        public static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getDepartment(), user.getRole(), user.isEnabled());
        }
    }
}
