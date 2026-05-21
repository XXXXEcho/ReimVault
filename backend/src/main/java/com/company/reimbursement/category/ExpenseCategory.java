package com.company.reimbursement.category;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "expense_categories")
public class ExpenseCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private boolean enabled;
    private int sortOrder;
    private String remark;
    private Instant createdAt;
    private Instant updatedAt;

    protected ExpenseCategory() {
    }

    public static ExpenseCategory create(String name, boolean enabled, int sortOrder, String remark) {
        ExpenseCategory category = new ExpenseCategory();
        category.name = name;
        category.enabled = enabled;
        category.sortOrder = sortOrder;
        category.remark = remark;
        category.createdAt = Instant.now();
        category.updatedAt = category.createdAt;
        return category;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public String getRemark() {
        return remark;
    }

    public void update(String name, boolean enabled, int sortOrder, String remark) {
        this.name = name;
        this.enabled = enabled;
        this.sortOrder = sortOrder;
        this.remark = remark;
        this.updatedAt = Instant.now();
    }
}
