package com.company.reimbursement.attachment;

import com.company.reimbursement.reimbursement.ReimbursementRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface ReimbursementAttachmentRepository extends JpaRepository<ReimbursementAttachment, Long> {
    int countByRecordAndType(ReimbursementRecord record, AttachmentType type);

    List<ReimbursementAttachment> findByRecord(ReimbursementRecord record);

    @Transactional
    void deleteByRecord(ReimbursementRecord record);
}
