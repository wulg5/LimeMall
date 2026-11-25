package com.limemall.controller;

import com.limemall.common.PageResult;
import com.limemall.common.Result;
import com.limemall.dto.FlashSaleSaveDTO;
import com.limemall.entity.SmsFlashSale;
import com.limemall.service.FlashSaleService;
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
 * 后台秒杀管理接口。
 */
@Tag(name = "后台-秒杀管理")
@RestController
@RequestMapping("/admin/flash-sales")
@RequiredArgsConstructor
public class AdminFlashSaleController {

    private final FlashSaleService flashSaleService;

    @GetMapping
    @PreAuthorize("hasAuthority('sms:flash:list')")
    @Operation(summary = "秒杀活动分页")
    public Result<PageResult<SmsFlashSale>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                 @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(flashSaleService.page(pageNum, pageSize));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('sms:flash:edit')")
    @Operation(summary = "新增秒杀活动")
    public Result<Void> create(@Valid @RequestBody FlashSaleSaveDTO dto) {
        dto.setId(null);
        flashSaleService.save(dto);
        return Result.ok();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('sms:flash:edit')")
    @Operation(summary = "编辑秒杀活动")
    public Result<Void> update(@Valid @RequestBody FlashSaleSaveDTO dto) {
        flashSaleService.save(dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('sms:flash:delete')")
    @Operation(summary = "删除秒杀活动")
    public Result<Void> delete(@PathVariable Long id) {
        flashSaleService.delete(id);
        return Result.ok();
    }
}
