package com.company.reimbursement.export;

import com.company.reimbursement.attachment.AttachmentType;
import com.company.reimbursement.attachment.ReimbursementAttachment;
import com.company.reimbursement.attachment.ReimbursementAttachmentRepository;
import com.company.reimbursement.batch.ReimbursementBatch;
import com.company.reimbursement.batch.ReimbursementBatchItem;
import com.company.reimbursement.batch.ReimbursementBatchItemRepository;
import com.company.reimbursement.batch.ReimbursementBatchRepository;
import com.company.reimbursement.config.StorageProperties;
import com.company.reimbursement.reimbursement.ReimbursementRecord;
import jakarta.persistence.EntityNotFoundException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ZipExportService {
    private final ReimbursementBatchRepository batches;
    private final ReimbursementBatchItemRepository items;
    private final ReimbursementAttachmentRepository attachments;
    private final Path root;

    public ZipExportService(ReimbursementBatchRepository batches, ReimbursementBatchItemRepository items, ReimbursementAttachmentRepository attachments, StorageProperties storageProperties) {
        this.batches = batches;
        this.items = items;
        this.attachments = attachments;
        this.root = Path.of(storageProperties.root());
    }

    @Transactional(readOnly = true)
    public byte[] exportBatchAttachments(Long batchId) {
        ReimbursementBatch batch = batches.findById(batchId).orElseThrow(() -> new EntityNotFoundException("批次不存在"));
        List<ReimbursementBatchItem> batchItems = items.findByBatchId(batchId).stream()
                .sorted(Comparator.comparing(ReimbursementBatchItem::getId))
                .toList();
        List<String> missing = new ArrayList<>();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(out)) {
            for (int i = 0; i < batchItems.size(); i++) {
                writeRecord(zip, batch, batchItems.get(i).getRecord(), i + 1, missing);
            }
            if (!missing.isEmpty()) {
                ZipEntry entry = new ZipEntry(rootName(batch) + "/附件缺失清单.txt");
                zip.putNextEntry(entry);
                zip.write(String.join(System.lineSeparator(), missing).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            zip.finish();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private void writeRecord(ZipOutputStream zip, ReimbursementBatch batch, ReimbursementRecord record, int sequence, List<String> missing) throws IOException {
        String recordPath = rootName(batch) + "/" + safe(record.getEmployee().getDisplayName()) + "/" + "%03d".formatted(sequence) + "-" + safe(record.getCategory().getName()) + "-" + amountText(record.getAmount());
        for (ReimbursementAttachment attachment : attachments.findByRecord(record)) {
            Path file = root.resolve(attachment.getStoragePath()).normalize();
            String entryName = recordPath + "/" + folderName(attachment.getType()) + "/" + safe(attachment.getOriginalFilename());
            if (!Files.exists(file)) {
                missing.add(entryName);
                continue;
            }
            zip.putNextEntry(new ZipEntry(entryName));
            Files.copy(file, zip);
            zip.closeEntry();
        }
    }

    private String rootName(ReimbursementBatch batch) {
        return "报销批次-" + safe(batch.getName());
    }

    private String folderName(AttachmentType type) {
        return switch (type) {
            case PAYMENT_VOUCHER -> "支付凭证";
            case ORDER_SCREENSHOT -> "订单截图";
            case INVOICE -> "发票";
        };
    }

    private String safe(String value) {
        return value.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String amountText(BigDecimal amount) {
        return amount.setScale(2).toPlainString();
    }
}
