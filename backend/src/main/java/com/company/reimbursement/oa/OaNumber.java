package com.company.reimbursement.oa;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "oa_numbers")
public class OaNumber {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String number;
    private Instant createdAt;

    protected OaNumber() {
    }

    public static OaNumber create(String number) {
        OaNumber oa = new OaNumber();
        oa.number = number;
        oa.createdAt = Instant.now();
        return oa;
    }

    public Long getId() { return id; }
    public String getNumber() { return number; }
    public Instant getCreatedAt() { return createdAt; }

    public void setNumber(String number) { this.number = number; }
}
