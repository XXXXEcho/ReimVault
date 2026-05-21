package com.company.reimbursement.attachment;

import com.company.reimbursement.reimbursement.ReimbursementRecord;
import com.company.reimbursement.reimbursement.ReimbursementRepository;
import com.company.reimbursement.reimbursement.ReimbursementStatus;
import com.company.reimbursement.user.User;
import com.company.reimbursement.user.UserRepository;
import com.company.reimbursement.user.UserRole;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class AttachmentController {
    private final ReimbursementRepository records;
    private final ReimbursementAttachmentRepository attachments;
    private final UserRepository users;
    private final FileStorageService storage;

    public AttachmentController(ReimbursementRepository records, ReimbursementAttachmentRepository attachments, UserRepository users, FileStorageService storage) {
        this.records = records;
        this.attachments = attachments;
        this.users = users;
        this.storage = storage;
    }

    @PostMapping("/api/reimbursements/{id}/attachments")
    @Transactional
    AttachmentResponse upload(@PathVariable Long id, @RequestParam AttachmentType type, @RequestParam MultipartFile file, Authentication authentication) {
        User user = findUser(authentication.getName());
        ReimbursementRecord record = records.findById(id).orElseThrow(() -> new EntityNotFoundException("报销记录不存在"));
        ensureOwner(record, user);
        if (record.getStatus() != ReimbursementStatus.DRAFT) {
            throw new IllegalArgumentException("只能给草稿记录上传附件");
        }
        FileStorageService.StoredFile stored = storage.save(record.getId(), type, file);
        ReimbursementAttachment attachment = attachments.save(ReimbursementAttachment.create(
                record, type, stored.originalFilename(), stored.storagePath(), stored.contentType(), stored.sizeBytes()
        ));
        return AttachmentResponse.from(attachment);
    }

    @GetMapping("/api/attachments/{id}")
    @Transactional(readOnly = true)
    ResponseEntity<Resource> download(@PathVariable Long id, Authentication authentication) throws IOException {
        User user = findUser(authentication.getName());
        ReimbursementAttachment attachment = attachments.findById(id).orElseThrow(() -> new EntityNotFoundException("附件不存在"));
        if (user.getRole() != UserRole.ADMIN) {
            ensureOwner(attachment.getRecord(), user);
        }
        Resource resource = storage.load(attachment.getStoragePath());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getOriginalFilename() + "\"")
                .body(resource);
    }

    private User findUser(String username) {
        return users.findByUsername(username).orElseThrow(() -> new EntityNotFoundException("用户不存在"));
    }

    private void ensureOwner(ReimbursementRecord record, User user) {
        if (!record.getEmployee().getId().equals(user.getId())) {
            throw new SecurityException("不能访问他人的报销记录");
        }
    }

    record AttachmentResponse(Long id, AttachmentType type, String originalFilename, String contentType, long sizeBytes) {
        static AttachmentResponse from(ReimbursementAttachment attachment) {
            return new AttachmentResponse(attachment.getId(), attachment.getType(), attachment.getOriginalFilename(), attachment.getContentType(), attachment.getSizeBytes());
        }
    }
}
