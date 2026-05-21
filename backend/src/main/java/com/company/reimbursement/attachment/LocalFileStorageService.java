package com.company.reimbursement.attachment;

import com.company.reimbursement.config.StorageProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalFileStorageService implements FileStorageService {
    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "application/pdf");

    private final Path root;

    public LocalFileStorageService(StorageProperties properties) {
        this.root = Path.of(properties.root());
    }

    @Override
    public StoredFile save(Long recordId, AttachmentType type, MultipartFile file) {
        validate(file);
        String safeName = file.getOriginalFilename() == null ? "attachment" : file.getOriginalFilename().replaceAll("[\\\\/:*?\"<>|]", "_");
        String typeFolder = type.name().toLowerCase();
        String relativePath = recordId + "/" + typeFolder + "/" + UUID.randomUUID() + "-" + safeName;
        Path target = root.resolve(relativePath).normalize();
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return new StoredFile(relativePath, safeName, file.getContentType(), file.getSize());
    }

    @Override
    public Resource load(String storagePath) {
        try {
            return new UrlResource(root.resolve(storagePath).normalize().toUri());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            Files.deleteIfExists(root.resolve(storagePath).normalize());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public boolean exists(String storagePath) {
        return Files.exists(root.resolve(storagePath).normalize());
    }

    private void validate(MultipartFile file) {
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("附件不能超过10MB");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("只支持图片或PDF附件");
        }
    }
}
