package com.company.reimbursement.user;

import com.company.reimbursement.reimbursement.ReimbursementRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Comparator;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository users;
    private final ReimbursementRepository records;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, ReimbursementRepository records, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.records = records;
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

    @Transactional
    public UserDtos.UserResponse update(Long id, UserDtos.UpdateUserRequest request) {
        User user = users.findById(id).orElseThrow(() -> new EntityNotFoundException("用户不存在"));
        user.update(request.displayName(), request.department(), request.role(), request.enabled());
        if (request.password() != null && !request.password().isBlank()) {
            user.changePassword(passwordEncoder.encode(request.password()));
        }
        return UserDtos.UserResponse.from(user);
    }

    @Transactional
    public void delete(Long id) {
        User user = users.findById(id).orElseThrow(() -> new EntityNotFoundException("用户不存在"));
        if (!records.findByEmployeeOrderByCreatedAtDesc(user).isEmpty()) {
            throw new IllegalArgumentException("该用户有关联报销记录，无法删除，请禁用账号");
        }
        users.delete(user);
    }
}
