package com.company.reimbursement.user;

import jakarta.persistence.EntityNotFoundException;
import java.util.Comparator;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserDtos.UserResponse create(UserDtos.CreateUserRequest request) {
        if (users.existsByUsername(request.username())) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User user = User.create(
                request.username(),
                request.displayName(),
                request.department(),
                passwordEncoder.encode(request.password()),
                request.role()
        );
        return UserDtos.UserResponse.from(users.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserDtos.UserResponse> list() {
        return users.findAll().stream()
                .sorted(Comparator.comparing(User::getId))
                .map(UserDtos.UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserDtos.UserResponse> searchEmployees(String keyword) {
        String cleaned = keyword == null ? "" : keyword.trim();
        List<User> matched = cleaned.isEmpty()
                ? users.findTop20ByRoleAndEnabledTrueOrderByDisplayNameAsc(UserRole.EMPLOYEE)
                : users.findTop20ByRoleAndEnabledTrueAndDisplayNameContainingIgnoreCaseOrderByDisplayNameAsc(UserRole.EMPLOYEE, cleaned);
        return matched.stream().map(UserDtos.UserResponse::from).toList();
    }

    @Transactional
    public UserDtos.UserResponse update(Long id, UserDtos.UpdateUserRequest request) {
        User user = users.findById(id).orElseThrow(() -> new EntityNotFoundException("用户不存在"));
        user.update(request.displayName(), request.department(), request.role(), request.enabled());
        return UserDtos.UserResponse.from(user);
    }
}
