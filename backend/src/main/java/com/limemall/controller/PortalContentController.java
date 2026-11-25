package com.limemall.controller;

import com.limemall.common.Result;
import com.limemall.entity.CmsBanner;
import com.limemall.entity.CmsNotice;
import com.limemall.service.ContentService;
import com.limemall.service.FlashSaleService;
import com.limemall.vo.FlashSaleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商城前台公开内容接口：轮播、公告、秒杀（免登录）。
 */
@Tag(name = "前台-内容")
@RestController
@RequestMapping("/portal/public")
@RequiredArgsConstructor
public class PortalContentController {

    private final ContentService contentService;
    private final FlashSaleService flashSaleService;

    @GetMapping("/banners")
    @Operation(summary = "首页轮播图")
    public Result<List<CmsBanner>> banners() {
        return Result.ok(contentService.activeBanners());
    }

    @GetMapping("/notices")
    @Operation(summary = "商城公告")
    public Result<List<CmsNotice>> notices() {
        return Result.ok(contentService.activeNotices());
    }

    @GetMapping("/flash-sales")
    @Operation(summary = "进行中的秒杀活动")
    public Result<List<FlashSaleVO>> flashSales() {
        return Result.ok(flashSaleService.activeList());
    }
}
