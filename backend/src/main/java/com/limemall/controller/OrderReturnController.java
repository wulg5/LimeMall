package com.limemall.controller;

import com.limemall.common.PageResult;
import com.limemall.common.Result;
import com.limemall.dto.ReturnHandleDTO;
import com.limemall.service.OrderReturnService;
import com.limemall.vo.ReturnVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 退款审核接口。
 */
@Tag(name = "后台-退款审核")
@RestController
@RequestMapping("/admin/returns")
@RequiredArgsConstructor
public class OrderReturnController {

    private final OrderReturnService orderReturnService;

    @GetMapping
    @PreAuthorize("hasAuthority('oms:return:list')")
    @Operation(summary = "分页查询退款申请")
    public Result<PageResult<ReturnVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "10") long pageSize,
                                             @RequestParam(required = false) Integer status,
                                             @RequestParam(required = false) String orderSn) {
        return Result.ok(orderReturnService.page(pageNum, pageSize, status, orderSn));
    }

    @PutMapping("/{id}/handle")
    @PreAuthorize("hasAuthority('oms:return:handle')")
    @Operation(summary = "审核退款申请")
    public Result<Void> handle(@PathVariable Long id, @Valid @RequestBody ReturnHandleDTO dto) {
        orderReturnService.handle(id, dto);
        return Result.ok();
    }

    @PutMapping("/{id}/returned-stock")
    @PreAuthorize("hasAuthority('oms:return:handle')")
    @Operation(summary = "确认退货入库并提交退款")
    public Result<Void> confirmReturnedStock(@PathVariable Long id) {
        orderReturnService.confirmReturnedStock(id);
        return Result.ok();
    }
}
