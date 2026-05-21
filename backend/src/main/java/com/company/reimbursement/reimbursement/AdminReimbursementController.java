package com.company.reimbursement.reimbursement;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
    List<ReimbursementDtos.RecordResponse> list() {
        return service.listAll();
    }

    @PatchMapping("/{id}/remark")
    ReimbursementDtos.RecordResponse updateRemark(@PathVariable Long id, @RequestBody ReimbursementDtos.AdminRemarkRequest request) {
        return service.updateAdminRemark(id, request);
    }
}
