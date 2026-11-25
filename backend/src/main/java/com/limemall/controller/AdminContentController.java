package com.limemall.controller;

import com.limemall.common.Result;
import com.limemall.dto.BannerSaveDTO;
import com.limemall.dto.NoticeSaveDTO;
import com.limemall.entity.CmsBanner;
import com.limemall.entity.CmsNotice;
import com.limemall.service.ContentService;
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
 * 后台内容运营接口：轮播图与公告管理。
 */
@Tag(name = "后台-内容管理")
@RestController
@RequestMapping("/admin/contents")
@RequiredArgsConstructor
public class AdminContentController {

    private final ContentService contentService;

    // ---------- 轮播图 ----------

    @GetMapping("/banners")
    @PreAuthorize("hasAuthority('cms:content:list')")
    @Operation(summary = "轮播图列表")
    public Result<List<CmsBanner>> banners() {
        return Result.ok(contentService.allBanners());
    }

    @PostMapping("/banners")
    @PreAuthorize("hasAuthority('cms:content:edit')")
    @Operation(summary = "新增轮播图")
    public Result<Void> createBanner(@Valid @RequestBody BannerSaveDTO dto) {
        dto.setId(null);
        contentService.saveBanner(dto);
        return Result.ok();
    }

    @PutMapping("/banners")
    @PreAuthorize("hasAuthority('cms:content:edit')")
    @Operation(summary = "编辑轮播图")
    public Result<Void> updateBanner(@Valid @RequestBody BannerSaveDTO dto) {
        contentService.saveBanner(dto);
        return Result.ok();
    }

    @DeleteMapping("/banners/{id}")
    @PreAuthorize("hasAuthority('cms:content:edit')")
    @Operation(summary = "删除轮播图")
    public Result<Void> deleteBanner(@PathVariable Long id) {
        contentService.deleteBanner(id);
        return Result.ok();
    }

    // ---------- 公告 ----------

    @GetMapping("/notices")
    @PreAuthorize("hasAuthority('cms:content:list')")
    @Operation(summary = "公告列表")
    public Result<List<CmsNotice>> notices() {
        return Result.ok(contentService.allNotices());
    }

    @PostMapping("/notices")
    @PreAuthorize("hasAuthority('cms:content:edit')")
    @Operation(summary = "新增公告")
    public Result<Void> createNotice(@Valid @RequestBody NoticeSaveDTO dto) {
        dto.setId(null);
        contentService.saveNotice(dto);
        return Result.ok();
    }

    @PutMapping("/notices")
    @PreAuthorize("hasAuthority('cms:content:edit')")
    @Operation(summary = "编辑公告")
    public Result<Void> updateNotice(@Valid @RequestBody NoticeSaveDTO dto) {
        contentService.saveNotice(dto);
        return Result.ok();
    }

    @DeleteMapping("/notices/{id}")
    @PreAuthorize("hasAuthority('cms:content:edit')")
    @Operation(summary = "删除公告")
    public Result<Void> deleteNotice(@PathVariable Long id) {
        contentService.deleteNotice(id);
        return Result.ok();
    }
}
