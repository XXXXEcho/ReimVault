package com.company.reimbursement.category;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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
    @PreAuthorize("hasAnyRole('ADMIN','SPECIALIST')")
    List<CategoryDtos.CategoryResponse> listAll() {
        return service.listAll();
    }

    @PostMapping("/api/admin/categories")
    @PreAuthorize("hasAnyRole('ADMIN','SPECIALIST')")
    CategoryDtos.CategoryResponse create(@Valid @RequestBody CategoryDtos.CreateCategoryRequest request) {
        return service.create(request);
    }

    @PatchMapping("/api/admin/categories/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SPECIALIST')")
    CategoryDtos.CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryDtos.UpdateCategoryRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/api/admin/categories/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SPECIALIST')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
