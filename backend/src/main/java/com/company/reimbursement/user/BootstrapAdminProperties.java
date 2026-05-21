package com.company.reimbursement.user;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap.admin")
public record BootstrapAdminProperties(String username, String password, String displayName, String department) {
    boolean enabled() {
        return username != null && !username.isBlank() && password != null && !password.isBlank();
    }
}
