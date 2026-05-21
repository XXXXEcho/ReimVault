package com.company.reimbursement.user;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapAdminRunner implements CommandLineRunner {
    private final BootstrapAdminProperties properties;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public BootstrapAdminRunner(BootstrapAdminProperties properties, UserRepository users, PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!properties.enabled() || users.existsByUsername(properties.username())) {
            return;
        }
        users.save(User.create(
                properties.username(),
                properties.displayName() == null || properties.displayName().isBlank() ? properties.username() : properties.displayName(),
                properties.department(),
                passwordEncoder.encode(properties.password()),
                UserRole.ADMIN
        ));
    }
}
