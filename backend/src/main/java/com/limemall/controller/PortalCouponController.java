package com.limemall.controller;

import com.limemall.common.Result;
import com.limemall.security.SecurityUtils;
import com.limemall.service.CouponService;
import com.limemall.vo.MemberCouponVO;
import com.limemall.vo.PortalCouponVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商城前台优惠券接口（需登录）。
 */
@Tag(name = "前台-优惠券")
@RestController
@RequestMapping("/portal/coupons")
@RequiredArgsConstructor
public class PortalCouponController {

    private final CouponService couponService;

    @GetMapping("/available")
    @Operation(summary = "领券中心（可领取优惠券）")
    public Result<List<PortalCouponVO>> available() {
        return Result.ok(couponService.available(SecurityUtils.getMemberId()));
    }

    @PostMapping("/{id}/receive")
    @Operation(summary = "领取优惠券")
    public Result<Void> receive(@PathVariable Long id) {
        couponService.receive(SecurityUtils.getMemberId(), id);
        return Result.ok();
    }

    @GetMapping("/mine")
    @Operation(summary = "我的优惠券")
    public Result<List<MemberCouponVO>> mine() {
        return Result.ok(couponService.mine(SecurityUtils.getMemberId()));
    }

    @GetMapping("/exchange")
    @Operation(summary = "积分商城可兑换优惠券")
    public Result<List<PortalCouponVO>> exchangeList() {
        return Result.ok(couponService.exchangeList());
    }

    @PostMapping("/{id}/exchange")
    @Operation(summary = "积分兑换优惠券")
    public Result<Void> exchange(@PathVariable Long id) {
        couponService.exchange(SecurityUtils.getMemberId(), id);
        return Result.ok();
    }
}
