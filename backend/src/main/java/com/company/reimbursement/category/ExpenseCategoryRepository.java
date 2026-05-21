package com.company.reimbursement.category;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {
    List<ExpenseCategory> findByEnabledTrueOrderBySortOrderAscNameAsc();

    boolean existsByName(String name);
}
