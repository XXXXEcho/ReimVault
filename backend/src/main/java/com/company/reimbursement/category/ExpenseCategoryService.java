package com.company.reimbursement.category;

import com.company.reimbursement.reimbursement.ReimbursementRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseCategoryService {
    private final ExpenseCategoryRepository categories;
    private final ReimbursementRepository records;

    public ExpenseCategoryService(ExpenseCategoryRepository categories, ReimbursementRepository records) {
        this.categories = categories;
        this.records = records;
    }

    @Transactional
    public CategoryDtos.CategoryResponse create(CategoryDtos.CreateCategoryRequest request) {
        if (categories.existsByName(request.name())) {
            throw new IllegalArgumentException("分类名称已存在");
        }
        ExpenseCategory category = ExpenseCategory.create(request.name(), request.enabled(), request.sortOrder(), request.remark());
        return CategoryDtos.CategoryResponse.from(categories.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryDtos.CategoryResponse> listEnabled() {
        return categories.findByEnabledTrueOrderBySortOrderAscNameAsc().stream()
                .map(CategoryDtos.CategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryDtos.CategoryResponse> listAll() {
        return categories.findAll().stream()
                .sorted(Comparator.comparingInt(ExpenseCategory::getSortOrder).thenComparing(ExpenseCategory::getName))
                .map(CategoryDtos.CategoryResponse::from)
                .toList();
    }

    @Transactional
    public CategoryDtos.CategoryResponse update(Long id, CategoryDtos.UpdateCategoryRequest request) {
        ExpenseCategory category = categories.findById(id).orElseThrow(() -> new EntityNotFoundException("分类不存在"));
        category.update(request.name(), request.enabled(), request.sortOrder(), request.remark());
        return CategoryDtos.CategoryResponse.from(category);
    }

    @Transactional
    public void delete(Long id) {
        ExpenseCategory category = categories.findById(id).orElseThrow(() -> new EntityNotFoundException("分类不存在"));
        if (records.existsByCategory_Id(id)) {
            throw new IllegalArgumentException("该分类有关联报销记录，无法删除");
        }
        categories.delete(category);
    }
}
