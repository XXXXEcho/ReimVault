package com.company.reimbursement.export;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/batches")
@PreAuthorize("hasAnyRole('ADMIN','SPECIALIST')")
public class ExportController {
    private final ExcelExportService excelExportService;
    private final ZipExportService zipExportService;

    public ExportController(ExcelExportService excelExportService, ZipExportService zipExportService) {
        this.excelExportService = excelExportService;
        this.zipExportService = zipExportService;
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
}
