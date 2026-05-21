package com.company.reimbursement.reimbursement;

import com.company.reimbursement.user.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReimbursementRepository extends JpaRepository<ReimbursementRecord, Long> {
    List<ReimbursementRecord> findByEmployeeOrderByCreatedAtDesc(User employee);
}
