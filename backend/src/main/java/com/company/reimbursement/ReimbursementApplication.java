package com.company.reimbursement;

import com.company.reimbursement.config.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class ReimbursementApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReimbursementApplication.class, args);
    }
}
