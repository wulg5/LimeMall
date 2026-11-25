package com.limemall.controller;

import com.limemall.common.PageResult;
import com.limemall.common.Result;
import com.limemall.dto.CouponSaveDTO;
import com.limemall.entity.SmsCoupon;
import com.limemall.service.CouponService;
import com.limemall.vo.CouponRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 优惠券管理接口。
 */
@Tag(name = "后台-优惠券管理")
@RestController
@RequestMapping("/admin/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping
    @PreAuthorize("hasAuthority('sms:coupon:list')")
    @Operation(summary = "分页查询优惠券")
    public Result<PageResult<SmsCoupon>> page(@RequestParam(defaultValue = "1") long pageNum,
                                              @RequestParam(defaultValue = "10") long pageSize,
                                              @RequestParam(required = false) String name,
                                              @RequestParam(required = false) Integer status) {
        return Result.ok(couponService.page(pageNum, pageSize, name, status));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('sms:coupon:create')")
    @Operation(summary = "新增优惠券")
    public Result<Void> create(@Valid @RequestBody CouponSaveDTO dto) {
        couponService.create(dto);
        return Result.ok();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('sms:coupon:update')")
    @Operation(summary = "编辑优惠券")
    public Result<Void> update(@Valid @RequestBody CouponSaveDTO dto) {
        couponService.update(dto);
        return Result.ok();
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('sms:coupon:publish')")
    @Operation(summary = "上线/下线优惠券")
    public Result<Void> publish(@PathVariable Long id, @RequestParam Integer status) {
        couponService.publish(id, status);
        return Result.ok();
    }

    @GetMapping("/{id}/records")
    @PreAuthorize("hasAuthority('sms:coupon:list')")
    @Operation(summary = "优惠券领取记录")
    public Result<PageResult<CouponRecordVO>> records(@PathVariable Long id,
                                                      @RequestParam(defaultValue = "1") long pageNum,
                                                      @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(couponService.records(id, pageNum, pageSize));
    }
}
