package com.company.reimbursement.category;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CategoryController {
    private final ExpenseCategoryService service;

    public CategoryController(ExpenseCategoryService service) {
        this.service = service;
    }

    @GetMapping("/api/categories")
    List<CategoryDtos.CategoryResponse> listEnabled() {
        return service.listEnabled();
    }

    @GetMapping("/api/admin/categories")
    @PreAuthorize("hasRole('ADMIN')")
    List<CategoryDtos.CategoryResponse> listAll() {
        return service.listAll();
    }

    @PostMapping("/api/admin/categories")
    @PreAuthorize("hasRole('ADMIN')")
    CategoryDtos.CategoryResponse create(@Valid @RequestBody CategoryDtos.CreateCategoryRequest request) {
        return service.create(request);
    }

    @PatchMapping("/api/admin/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    CategoryDtos.CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryDtos.UpdateCategoryRequest request) {
        return service.update(id, request);
    }
}
