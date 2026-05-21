package com.company.reimbursement.attachment;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    StoredFile save(Long recordId, AttachmentType type, MultipartFile file);

    Resource load(String storagePath);

    void delete(String storagePath);

    boolean exists(String storagePath);

    record StoredFile(String storagePath, String originalFilename, String contentType, long sizeBytes) {
    }
}
