package com.limemall.controller;

import com.limemall.common.PageResult;
import com.limemall.common.Result;
import com.limemall.dto.ProductSaveDTO;
import com.limemall.service.ProductService;
import com.limemall.vo.ProductDetailVO;
import com.limemall.vo.ProductVO;
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

/**
 * 商品管理接口（SPU + SKU）。
 */
@Tag(name = "后台-商品管理")
@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @PreAuthorize("hasAuthority('pms:product:list')")
    @Operation(summary = "分页查询商品")
    public Result<PageResult<ProductVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                              @RequestParam(defaultValue = "10") long pageSize,
                                              @RequestParam(required = false) String name,
                                              @RequestParam(required = false) Long categoryId,
                                              @RequestParam(required = false) Long brandId,
                                              @RequestParam(required = false) Integer publishStatus) {
        return Result.ok(productService.page(pageNum, pageSize, name, categoryId, brandId, publishStatus));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('pms:product:list')")
    @Operation(summary = "商品详情（含SKU）")
    public Result<ProductDetailVO> detail(@PathVariable Long id) {
        return Result.ok(productService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('pms:product:create')")
    @Operation(summary = "新增商品")
    public Result<Void> create(@Valid @RequestBody ProductSaveDTO dto) {
        productService.create(dto);
        return Result.ok();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('pms:product:update')")
    @Operation(summary = "编辑商品")
    public Result<Void> update(@Valid @RequestBody ProductSaveDTO dto) {
        productService.update(dto);
        return Result.ok();
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('pms:product:publish')")
    @Operation(summary = "上架/下架商品")
    public Result<Void> publish(@PathVariable Long id, @RequestParam Integer status) {
        productService.updatePublishStatus(id, status);
        return Result.ok();
    }

    @PutMapping("/skus/{skuId}/stock")
    @PreAuthorize("hasAuthority('pms:product:update')")
    @Operation(summary = "调整 SKU 库存")
    public Result<Void> updateSkuStock(@PathVariable Long skuId, @RequestParam Integer stock) {
        productService.updateSkuStock(skuId, stock);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('pms:product:delete')")
    @Operation(summary = "删除商品（逻辑删除）")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.ok();
    }
}
