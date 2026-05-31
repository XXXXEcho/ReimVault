package com.company.reimbursement.export;

import com.company.reimbursement.attachment.AttachmentType;
import com.company.reimbursement.attachment.FileStorageService;
import com.company.reimbursement.attachment.ReimbursementAttachment;
import com.company.reimbursement.attachment.ReimbursementAttachmentRepository;
import com.company.reimbursement.batch.ReimbursementBatch;
import com.company.reimbursement.batch.ReimbursementBatchItem;
import com.company.reimbursement.batch.ReimbursementBatchItemRepository;
import com.company.reimbursement.batch.ReimbursementBatchRepository;
import com.company.reimbursement.reimbursement.ReimbursementRecord;
import jakarta.persistence.EntityNotFoundException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExcelExportService {
    private static final List<String> HEADERS = List.of("批次名称", "员工姓名", "部门", "金额", "用途分类", "用途说明", "经费编码", "支付时间", "支付凭证数量", "订单截图数量", "发票数量", "提交时间", "报销状态", "报销时间", "管理员备注", "附件目录路径");
    private static final ZoneId CN = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(CN);

    private final ReimbursementBatchRepository batches;
    private final ReimbursementBatchItemRepository items;
    private final ReimbursementAttachmentRepository attachments;
    private final FileStorageService storage;

    public ExcelExportService(ReimbursementBatchRepository batches, ReimbursementBatchItemRepository items, ReimbursementAttachmentRepository attachments, FileStorageService storage) {
        this.batches = batches;
        this.items = items;
        this.attachments = attachments;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public byte[] exportBatch(Long batchId) {
        ReimbursementBatch batch = batches.findById(batchId).orElseThrow(() -> new EntityNotFoundException("批次不存在"));
        List<ReimbursementBatchItem> batchItems = items.findByBatchId(batchId).stream()
                .sorted(Comparator.comparing(ReimbursementBatchItem::getId))
                .toList();
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("报销清单");
            writeHeader(sheet.createRow(0));
            for (int i = 0; i < batchItems.size(); i++) {
                writeRow(sheet.createRow(i + 1), batch, batchItems.get(i), i + 1);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private void writeHeader(Row row) {
        for (int i = 0; i < HEADERS.size(); i++) {
            row.createCell(i).setCellValue(HEADERS.get(i));
        }
    }

    private void writeRow(Row row, ReimbursementBatch batch, ReimbursementBatchItem item, int sequence) {
        ReimbursementRecord record = item.getRecord();
        List<ReimbursementAttachment> recordAttachments = attachments.findByRecord(record);
        row.createCell(0).setCellValue(batch.getName());
        row.createCell(1).setCellValue(record.getEmployee().getDisplayName());
        row.createCell(2).setCellValue(record.getEmployee().getDepartment());
        row.createCell(3).setCellValue(record.getAmount().doubleValue());
        row.createCell(4).setCellValue(record.getCategory().getName());
        row.createCell(5).setCellValue(record.getPurpose());
        row.createCell(6).setCellValue(record.getOa() != null ? record.getOa().getNumber() : "");
        row.createCell(7).setCellValue(FMT.format(record.getPaymentTime()));
        row.createCell(8).setCellValue(count(recordAttachments, AttachmentType.PAYMENT_VOUCHER));
        row.createCell(9).setCellValue(count(recordAttachments, AttachmentType.ORDER_SCREENSHOT));
        row.createCell(10).setCellValue(count(recordAttachments, AttachmentType.INVOICE));
        row.createCell(11).setCellValue(record.getSubmittedAt() == null ? "" : FMT.format(record.getSubmittedAt()));
        row.createCell(12).setCellValue(statusLabel(record));
        row.createCell(13).setCellValue(record.getReimbursedAt() == null ? "" : FMT.format(record.getReimbursedAt()));
        row.createCell(14).setCellValue(record.getAdminRemark() == null ? "" : record.getAdminRemark());
        row.createCell(15).setCellValue(attachmentDirectory(record, recordAttachments, sequence));
    }

    private long count(List<ReimbursementAttachment> attachments, AttachmentType type) {
        return attachments.stream().filter(attachment -> attachment.getType() == type).count();
    }

    private String attachmentDirectory(ReimbursementRecord record, List<ReimbursementAttachment> recordAttachments, int sequence) {
        String path = record.getEmployee().getDisplayName() + "/" + "%03d".formatted(sequence) + "-" + record.getCategory().getName() + "-" + amountText(record.getAmount());
        boolean missing = recordAttachments.stream().anyMatch(attachment -> !storage.exists(attachment.getStoragePath()));
        return missing ? path + "（附件缺失）" : path;
    }

    private String amountText(BigDecimal amount) {
        return amount.setScale(2).toPlainString();
    }

    private String statusLabel(ReimbursementRecord record) {
        if (record.getReimbursedAt() != null) return "已报销";
        return switch (record.getStatus()) {
            case DRAFT -> "未提交";
            case SUBMITTED -> "已提交未报销";
            case ARCHIVED -> "已报销";
        };
    }
}
