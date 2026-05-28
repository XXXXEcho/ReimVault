package com.company.reimbursement.batch;

import java.time.YearMonth;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/batches")
@PreAuthorize("hasAnyRole('ADMIN','SPECIALIST')")
public class BatchController {
    private final BatchService service;

    public BatchController(BatchService service) {
        this.service = service;
    }

    @PostMapping
    BatchDtos.BatchResponse create(@RequestBody BatchDtos.CreateBatchRequest request, Authentication authentication) {
        return service.create(authentication.getName(), request);
    }

    @GetMapping
    List<BatchDtos.BatchResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    BatchDtos.BatchResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/items/{recordId}")
    BatchDtos.BatchResponse addItem(@PathVariable Long id, @PathVariable Long recordId) {
        return service.addItem(id, recordId);
    }

    @DeleteMapping("/{id}/items/{recordId}")
    BatchDtos.BatchResponse removeItem(@PathVariable Long id, @PathVariable Long recordId) {
        return service.removeItem(id, recordId);
    }

    @PostMapping("/{id}/archive")
    BatchDtos.BatchResponse archive(@PathVariable Long id) {
        return service.archive(id);
    }

    @PostMapping("/monthly")
    BatchDtos.BatchResponse ensureMonthly() {
        return toResponse(service.ensureMonthlyBatch(YearMonth.now()));
    }

    private BatchDtos.BatchResponse toResponse(ReimbursementBatch batch) {
        return service.get(batch.getId());
    }
}
