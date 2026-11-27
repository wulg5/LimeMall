package com.limemall.controller;

import com.limemall.common.PageResult;
import com.limemall.common.Result;
import com.limemall.dto.DeliverDTO;
import com.limemall.service.OrderService;
import com.limemall.vo.OrderDetailVO;
import com.limemall.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 订单管理接口（查询/发货/关闭）。
 */
@Tag(name = "后台-订单管理")
@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    @PreAuthorize("hasAuthority('oms:order:list')")
    @Operation(summary = "分页查询订单")
    public Result<PageResult<OrderVO>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String orderSn,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(orderService.page(pageNum, pageSize, orderSn, status, beginDate, endDate));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('oms:order:list')")
    @Operation(summary = "订单详情")
    public Result<OrderDetailVO> detail(@PathVariable Long id) {
        return Result.ok(orderService.detail(id));
    }

    @PostMapping("/{id}/deliver")
    @PreAuthorize("hasAuthority('oms:order:deliver')")
    @Operation(summary = "订单发货")
    public Result<Void> deliver(@PathVariable Long id, @Valid @RequestBody DeliverDTO dto) {
        orderService.deliver(id, dto);
        return Result.ok();
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("hasAuthority('oms:order:close')")
    @Operation(summary = "关闭待付款订单（回补库存与优惠券）")
    public Result<Void> close(@PathVariable Long id) {
        orderService.close(id);
        return Result.ok();
    }
}
