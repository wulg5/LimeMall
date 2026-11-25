package com.limemall.controller;

import com.limemall.common.Result;
import com.limemall.service.DashboardService;
import com.limemall.vo.CategorySalesVO;
import com.limemall.vo.DashboardSummaryVO;
import com.limemall.vo.LatestOrderVO;
import com.limemall.vo.OrderStatusVO;
import com.limemall.vo.TopProductVO;
import com.limemall.vo.TrendPointVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据看板接口。
 */
@Tag(name = "后台-数据看板")
@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('dashboard:view')")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "顶部统计卡片")
    public Result<DashboardSummaryVO> summary() {
        return Result.ok(dashboardService.summary());
    }

    @GetMapping("/trend")
    @Operation(summary = "销售趋势（近 N 天）")
    public Result<List<TrendPointVO>> trend(@RequestParam(defaultValue = "30") int days) {
        return Result.ok(dashboardService.trend(days));
    }

    @GetMapping("/category-sales")
    @Operation(summary = "一级分类销售额占比")
    public Result<List<CategorySalesVO>> categorySales() {
        return Result.ok(dashboardService.categorySales());
    }

    @GetMapping("/top-products")
    @Operation(summary = "热销商品排行")
    public Result<List<TopProductVO>> topProducts(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(dashboardService.topProducts(limit));
    }

    @GetMapping("/order-status")
    @Operation(summary = "订单状态分布")
    public Result<List<OrderStatusVO>> orderStatus() {
        return Result.ok(dashboardService.orderStatusDistribution());
    }

    @GetMapping("/latest-orders")
    @Operation(summary = "最新订单")
    public Result<List<LatestOrderVO>> latestOrders(@RequestParam(defaultValue = "8") int limit) {
        return Result.ok(dashboardService.latestOrders(limit));
    }
}
