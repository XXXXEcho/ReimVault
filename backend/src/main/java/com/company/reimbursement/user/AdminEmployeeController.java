package com.company.reimbursement.user;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/employees")
@PreAuthorize("hasAnyRole('ADMIN','SPECIALIST')")
public class AdminEmployeeController {
    private final UserService service;

    public AdminEmployeeController(UserService service) {
        this.service = service;
    }

    @GetMapping
    List<UserDtos.UserResponse> search(@RequestParam(required = false) String keyword) {
        return service.searchEmployees(keyword);
    }
}
