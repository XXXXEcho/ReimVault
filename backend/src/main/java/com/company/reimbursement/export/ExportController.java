package com.company.reimbursement.export;

import com.company.reimbursement.reimbursement.ReimbursementRecord;
import com.company.reimbursement.reimbursement.ReimbursementService;
import java.util.Collections;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/batches")
@PreAuthorize("hasAnyRole('ADMIN','SPECIALIST')")
public class ExportController {
    private final ExcelExportService excelExportService;
    private final ZipExportService zipExportService;
    private final ReimbursementService reimbursementService;

    public ExportController(ExcelExportService excelExportService, ZipExportService zipExportService, ReimbursementService reimbursementService) {
        this.excelExportService = excelExportService;
        this.zipExportService = zipExportService;
        this.reimbursementService = reimbursementService;
    }

    @GetMapping("/{id}/export/excel")
    ResponseEntity<byte[]> exportExcel(@PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"batch-" + id + ".xlsx\"")
                .body(excelExportService.exportBatch(id));
    }

    @GetMapping("/{id}/export/attachments")
    ResponseEntity<byte[]> exportAttachments(@PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"batch-" + id + "-attachments.zip\"")
                .body(zipExportService.exportBatchAttachments(id));
    }

    @GetMapping("/export/excel")
    ResponseEntity<byte[]> exportFilteredExcel(
            @RequestParam(required = false) List<Long> oaIds,
            @RequestParam(required = false) List<String> months) {
        List<ReimbursementRecord> filtered = reimbursementService.listFiltered(
                oaIds != null ? oaIds : Collections.emptyList(),
                months != null ? months : Collections.emptyList());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"export.xlsx\"")
                .body(excelExportService.exportRecords(filtered));
    }

    @GetMapping("/export/attachments")
    ResponseEntity<byte[]> exportFilteredAttachments(
            @RequestParam(required = false) List<Long> oaIds,
            @RequestParam(required = false) List<String> months) {
        List<ReimbursementRecord> filtered = reimbursementService.listFiltered(
                oaIds != null ? oaIds : Collections.emptyList(),
                months != null ? months : Collections.emptyList());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"export-attachments.zip\"")
                .body(zipExportService.exportAttachmentsForRecords(filtered));
    }
}
