package com.limemall.controller;

import com.limemall.common.PageResult;
import com.limemall.common.Result;
import com.limemall.service.CommentService;
import com.limemall.service.PortalService;
import com.limemall.service.ProductService;
import com.limemall.vo.CategoryNodeVO;
import com.limemall.vo.PortalHomeVO;
import com.limemall.vo.ProductDetailVO;
import com.limemall.vo.ProductVO;
import com.limemall.vo.PublicCommentVO;
import com.limemall.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商城前台公开接口：首页、分类、商品浏览（无需登录）。
 */
@Tag(name = "前台-商品浏览")
@RestController
@RequestMapping("/portal/public")
@RequiredArgsConstructor
public class PortalProductController {

    private final PortalService portalService;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final CommentService commentService;

    @GetMapping("/home")
    @Operation(summary = "首页数据（分类树 + 热销 + 新品）")
    public Result<PortalHomeVO> home() {
        return Result.ok(portalService.home());
    }

    @GetMapping("/categories")
    @Operation(summary = "分类树")
    public Result<List<CategoryNodeVO>> categories() {
        return Result.ok(categoryService.tree());
    }

    @GetMapping("/products")
    @Operation(summary = "商品分页（仅上架商品，支持排序）")
    public Result<PageResult<ProductVO>> products(@RequestParam(defaultValue = "1") long pageNum,
                                                  @RequestParam(defaultValue = "12") long pageSize,
                                                  @RequestParam(required = false) String name,
                                                  @RequestParam(required = false) Long categoryId,
                                                  @RequestParam(required = false) Long brandId,
                                                  @RequestParam(required = false) String sort) {
        return Result.ok(productService.page(pageNum, pageSize, name, categoryId, brandId, 1, sort));
    }

    @GetMapping("/products/{id}")
    @Operation(summary = "商品详情（仅上架商品）")
    public Result<ProductDetailVO> detail(@PathVariable Long id) {
        ProductDetailVO detail = productService.detail(id);
        if (detail.getPublishStatus() == null || detail.getPublishStatus() != 1) {
            throw new com.limemall.common.BusinessException("商品已下架");
        }
        return Result.ok(detail);
    }

    @GetMapping("/products/{id}/comments")
    @Operation(summary = "商品评价分页（公开）")
    public Result<PageResult<PublicCommentVO>> comments(@PathVariable Long id,
                                                        @RequestParam(defaultValue = "1") long pageNum,
                                                        @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(commentService.productComments(id, pageNum, pageSize));
    }
}
