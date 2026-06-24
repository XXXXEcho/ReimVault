package com.company.reimbursement.reimbursement;

import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reimbursements")
@PreAuthorize("hasAnyRole('ADMIN','SPECIALIST')")
public class AdminReimbursementController {
    private final ReimbursementService service;

    public AdminReimbursementController(ReimbursementService service) {
        this.service = service;
    }

    @GetMapping
    List<ReimbursementDtos.RecordResponse> list(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) ReimbursementStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Boolean reimbursed,
            @RequestParam(required = false) Long oaId
    ) {
        return service.listAll(new ReimbursementDtos.AdminListFilter(employeeId, categoryId, status, from, to, reimbursed, oaId));
    }

    @GetMapping("/stats")
    ReimbursementDtos.StatsResponse stats(
            @RequestParam(required = false) List<Long> oaIds,
            @RequestParam(required = false) List<Long> batchIds) {
        return service.computeStats(
                oaIds != null ? oaIds : List.of(),
                batchIds != null ? batchIds : List.of());
    }

    @GetMapping("/{id}")
    ReimbursementDtos.RecordResponse get(@PathVariable Long id) {
        return service.getAny(id);
    }

    @PatchMapping("/{id}/remark")
    ReimbursementDtos.RecordResponse updateRemark(@PathVariable Long id, @RequestBody ReimbursementDtos.AdminRemarkRequest request) {
        return service.updateAdminRemark(id, request);
    }

    @PatchMapping("/{id}/oa-number")
    ReimbursementDtos.RecordResponse updateOaNumber(@PathVariable Long id, @RequestBody ReimbursementDtos.OaNumberRequest request) {
        return service.updateOaNumber(id, request);
    }

    @PostMapping("/{id}/reject")
    ReimbursementDtos.RecordResponse reject(@PathVariable Long id) {
        return service.reject(id);
    }

    @PostMapping("/{id}/reimburse")
    ReimbursementDtos.RecordResponse reimburse(@PathVariable Long id) {
        return service.markReimbursed(id);
    }

    @PostMapping("/{id}/unreimburse")
    ReimbursementDtos.RecordResponse unreimburse(@PathVariable Long id) {
        return service.clearReimbursed(id);
    }

    @PostMapping("/archive")
    void archive(@RequestBody ArchiveRequest request) {
        service.archiveRecords(request.ids());
    }

    public record ArchiveRequest(List<Long> ids) {
    }
}
