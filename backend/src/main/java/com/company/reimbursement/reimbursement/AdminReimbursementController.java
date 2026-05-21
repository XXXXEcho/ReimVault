package com.company.reimbursement.reimbursement;

import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reimbursements")
@PreAuthorize("hasRole('ADMIN')")
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return service.listAll(new ReimbursementDtos.AdminListFilter(employeeId, categoryId, status, from, to));
    }

    @PatchMapping("/{id}/remark")
    ReimbursementDtos.RecordResponse updateRemark(@PathVariable Long id, @RequestBody ReimbursementDtos.AdminRemarkRequest request) {
        return service.updateAdminRemark(id, request);
    }
}
