package com.company.reimbursement;

import com.company.reimbursement.config.StorageProperties;
import com.company.reimbursement.user.BootstrapAdminProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({StorageProperties.class, BootstrapAdminProperties.class})
@EnableScheduling
public class ReimbursementApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReimbursementApplication.class, args);
    }
}
