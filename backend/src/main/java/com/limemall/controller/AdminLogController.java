package com.limemall.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limemall.common.PageResult;
import com.limemall.common.Result;
import com.limemall.entity.SysAdminLog;
import com.limemall.mapper.SysAdminLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台操作日志查询接口（只读）。
 */
@Tag(name = "后台-操作日志")
@RestController
@RequestMapping("/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {

    private final SysAdminLogMapper adminLogMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('sys:log:list')")
    @Operation(summary = "操作日志分页")
    public Result<PageResult<SysAdminLog>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                @RequestParam(defaultValue = "10") long pageSize,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<SysAdminLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysAdminLog::getAdminUsername, keyword)
                    .or().like(SysAdminLog::getModule, keyword)
                    .or().like(SysAdminLog::getUri, keyword));
        }
        if (status != null) {
            wrapper.eq(SysAdminLog::getStatus, status);
        }
        wrapper.orderByDesc(SysAdminLog::getId);
        Page<SysAdminLog> page = adminLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return Result.ok(PageResult.of(page, page.getRecords()));
    }
}
