package com.company.reimbursement.reimbursement;

import com.company.reimbursement.user.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReimbursementRepository extends JpaRepository<ReimbursementRecord, Long>, JpaSpecificationExecutor<ReimbursementRecord> {
    List<ReimbursementRecord> findByEmployeeOrderByCreatedAtDesc(User employee);
}
