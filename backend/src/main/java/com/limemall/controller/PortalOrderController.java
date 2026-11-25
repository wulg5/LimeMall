package com.limemall.controller;

import com.limemall.common.PageResult;
import com.limemall.common.Result;
import com.limemall.dto.OrderCreateDTO;
import com.limemall.dto.ReturnApplyDTO;
import com.limemall.security.SecurityUtils;
import com.limemall.service.OrderReturnService;
import com.limemall.service.OrderService;
import com.limemall.vo.OrderDetailVO;
import com.limemall.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商城前台订单接口（需登录）。
 */
@Tag(name = "前台-订单")
@RestController
@RequestMapping("/portal/orders")
@RequiredArgsConstructor
public class PortalOrderController {

    private final OrderService orderService;
    private final OrderReturnService orderReturnService;

    @PostMapping
    @Operation(summary = "提交订单（扣库存 + 核销优惠券）")
    public Result<OrderDetailVO> create(@Valid @RequestBody OrderCreateDTO dto) {
        return Result.ok(orderService.createOrder(SecurityUtils.getMemberId(), dto));
    }

    @GetMapping
    @Operation(summary = "我的订单分页")
    public Result<PageResult<OrderVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize,
                                            @RequestParam(required = false) Integer status) {
        return Result.ok(orderService.pageMine(SecurityUtils.getMemberId(), pageNum, pageSize, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "我的订单详情")
    public Result<OrderDetailVO> detail(@PathVariable Long id) {
        return Result.ok(orderService.detailMine(SecurityUtils.getMemberId(), id));
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "模拟支付")
    public Result<Void> pay(@PathVariable Long id) {
        orderService.pay(SecurityUtils.getMemberId(), id);
        return Result.ok();
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消订单（仅待付款）")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancel(SecurityUtils.getMemberId(), id);
        return Result.ok();
    }

    @PostMapping("/{id}/receive")
    @Operation(summary = "确认收货")
    public Result<Void> receive(@PathVariable Long id) {
        orderService.receive(SecurityUtils.getMemberId(), id);
        return Result.ok();
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "申请退款（待发货/已发货订单）")
    public Result<Void> refund(@PathVariable Long id, @Valid @RequestBody ReturnApplyDTO dto) {
        dto.setOrderId(id);
        orderReturnService.apply(SecurityUtils.getMemberId(), dto);
        return Result.ok();
    }
}
