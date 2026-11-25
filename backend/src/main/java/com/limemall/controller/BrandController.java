package com.limemall.controller;

import com.limemall.common.PageResult;
import com.limemall.common.Result;
import com.limemall.dto.BrandSaveDTO;
import com.limemall.entity.PmsBrand;
import com.limemall.service.BrandService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 品牌管理接口。
 */
@Tag(name = "后台-品牌管理")
@RestController
@RequestMapping("/admin/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @GetMapping
    @PreAuthorize("hasAuthority('pms:brand:list')")
    @Operation(summary = "分页查询品牌")
    public Result<PageResult<PmsBrand>> page(@RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "10") long pageSize,
                                             @RequestParam(required = false) String keyword) {
        return Result.ok(brandService.page(pageNum, pageSize, keyword));
    }

    @GetMapping("/options")
    @PreAuthorize("hasAnyAuthority('pms:brand:list', 'pms:product:list')")
    @Operation(summary = "启用品牌下拉选项")
    public Result<List<PmsBrand>> options() {
        return Result.ok(brandService.options());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('pms:brand:create')")
    @Operation(summary = "新增品牌")
    public Result<Void> create(@Valid @RequestBody BrandSaveDTO dto) {
        brandService.create(dto);
        return Result.ok();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('pms:brand:update')")
    @Operation(summary = "编辑品牌")
    public Result<Void> update(@Valid @RequestBody BrandSaveDTO dto) {
        brandService.update(dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('pms:brand:delete')")
    @Operation(summary = "删除品牌")
    public Result<Void> delete(@PathVariable Long id) {
        brandService.delete(id);
        return Result.ok();
    }
}
