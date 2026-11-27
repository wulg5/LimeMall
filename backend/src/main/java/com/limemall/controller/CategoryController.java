package com.limemall.controller;

import com.limemall.common.Result;
import com.limemall.dto.CategorySaveDTO;
import com.limemall.service.CategoryService;
import com.limemall.vo.CategoryNodeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品分类接口。
 */
@Tag(name = "后台-分类管理")
@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/tree")
    @PreAuthorize("hasAnyAuthority('pms:category:list', 'pms:product:list')")
    @Operation(summary = "分类树")
    public Result<List<CategoryNodeVO>> tree() {
        return Result.ok(categoryService.adminTree());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('pms:category:create')")
    @Operation(summary = "新增分类")
    public Result<Void> create(@Valid @RequestBody CategorySaveDTO dto) {
        categoryService.create(dto);
        return Result.ok();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('pms:category:update')")
    @Operation(summary = "编辑分类")
    public Result<Void> update(@Valid @RequestBody CategorySaveDTO dto) {
        categoryService.update(dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('pms:category:delete')")
    @Operation(summary = "删除分类")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.ok();
    }
}
