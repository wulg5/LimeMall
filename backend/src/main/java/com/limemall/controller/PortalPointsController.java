package com.limemall.controller;

import com.limemall.common.PageResult;
import com.limemall.common.Result;
import com.limemall.entity.UmsPointsLog;
import com.limemall.security.SecurityUtils;
import com.limemall.service.PointsLedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商城前台积分接口：积分明细。
 */
@Tag(name = "前台-积分")
@RestController
@RequestMapping("/portal/points")
@RequiredArgsConstructor
public class PortalPointsController {

    private final PointsLedgerService pointsLedgerService;

    @GetMapping("/records")
    @Operation(summary = "积分明细（获取/消费/过期流水）")
    public Result<PageResult<UmsPointsLog>> records(@RequestParam(defaultValue = "1") long pageNum,
                                                    @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(pointsLedgerService.records(SecurityUtils.getMemberId(), pageNum, pageSize));
    }
}
