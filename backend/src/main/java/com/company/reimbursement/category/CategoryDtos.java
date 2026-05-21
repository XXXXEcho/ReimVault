package com.company.reimbursement.category;

import jakarta.validation.constraints.NotBlank;

public class CategoryDtos {
    public record CreateCategoryRequest(@NotBlank String name, boolean enabled, int sortOrder, String remark) {
    }

    public record UpdateCategoryRequest(@NotBlank String name, boolean enabled, int sortOrder, String remark) {
    }

    public record CategoryResponse(Long id, String name, boolean enabled, int sortOrder, String remark) {
        public static CategoryResponse from(ExpenseCategory category) {
            return new CategoryResponse(category.getId(), category.getName(), category.isEnabled(), category.getSortOrder(), category.getRemark());
        }
    }
}
