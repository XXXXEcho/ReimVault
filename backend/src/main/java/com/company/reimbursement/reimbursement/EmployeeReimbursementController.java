package com.company.reimbursement.reimbursement;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reimbursements")
public class EmployeeReimbursementController {
    private final ReimbursementService service;

    public EmployeeReimbursementController(ReimbursementService service) {
        this.service = service;
    }

    @GetMapping
    List<ReimbursementDtos.RecordResponse> list(Authentication authentication) {
        return service.listMine(authentication.getName());
    }

    @PostMapping
    ReimbursementDtos.RecordResponse create(@RequestBody ReimbursementDtos.SaveRecordRequest request, Authentication authentication) {
        return service.createDraft(authentication.getName(), request);
    }

    @GetMapping("/{id}")
    ReimbursementDtos.RecordResponse get(@PathVariable Long id, Authentication authentication) {
        return service.getMine(authentication.getName(), id);
    }

    @PatchMapping("/{id}")
    ReimbursementDtos.RecordResponse update(@PathVariable Long id, @RequestBody ReimbursementDtos.SaveRecordRequest request, Authentication authentication) {
        return service.updateDraft(authentication.getName(), id, request);
    }

    @PostMapping("/{id}/submit")
    ReimbursementDtos.RecordResponse submit(@PathVariable Long id, Authentication authentication) {
        return service.submit(authentication.getName(), id);
    }
}
