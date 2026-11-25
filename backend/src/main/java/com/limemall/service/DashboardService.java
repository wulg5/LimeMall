package com.limemall.service;

import com.limemall.mapper.DashboardMapper;
import com.limemall.vo.CategorySalesVO;
import com.limemall.vo.DashboardSummaryVO;
import com.limemall.vo.LatestOrderVO;
import com.limemall.vo.OrderStatusVO;
import com.limemall.vo.TopProductVO;
import com.limemall.vo.TrendPointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 数据看板服务：汇总统计、趋势、分类占比、热销排行等。
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardMapper dashboardMapper;

    /**
     * 看板顶部统计卡片。
     */
    public DashboardSummaryVO summary() {
        DashboardSummaryVO vo = new DashboardSummaryVO();
        vo.setTodayOrderCount(dashboardMapper.countTodayOrders());
        vo.setTodayPayAmount(dashboardMapper.sumTodayPayAmount());
        vo.setTodayNewMemberCount(dashboardMapper.countTodayNewMembers());
        vo.setProductCount(dashboardMapper.countOnSaleProducts());
        vo.setMemberCount(dashboardMapper.countMembers());
        vo.setPendingDeliverCount(dashboardMapper.countPendingDeliver());
        vo.setPendingReturnCount(dashboardMapper.countPendingReturns());
        return vo;
    }

    /**
     * 近 N 天销售趋势。数据库仅返回有订单的日期，服务层补齐空缺日期保证折线图连续。
     */
    public List<TrendPointVO> trend(int days) {
        int range = Math.max(7, Math.min(days, 90));
        LocalDate start = LocalDate.now().minusDays(range - 1L);
        Map<String, TrendPointVO> byDate = dashboardMapper.selectTrend(start).stream()
                .collect(Collectors.toMap(TrendPointVO::getDate, Function.identity()));
        List<TrendPointVO> result = new ArrayList<>(range);
        for (int i = 0; i < range; i++) {
            String date = start.plusDays(i).toString();
            result.add(byDate.getOrDefault(date,
                    new TrendPointVO(date, 0L, BigDecimal.ZERO)));
        }
        return result;
    }

    /** 一级分类销售额占比 */
    public List<CategorySalesVO> categorySales() {
        return dashboardMapper.selectCategorySales();
    }

    /** 热销商品排行 */
    public List<TopProductVO> topProducts(int limit) {
        return dashboardMapper.selectTopProducts(Math.max(1, Math.min(limit, 20)));
    }

    /** 订单状态分布 */
    public List<OrderStatusVO> orderStatusDistribution() {
        return dashboardMapper.selectOrderStatusDistribution();
    }

    /** 最新订单 */
    public List<LatestOrderVO> latestOrders(int limit) {
        return dashboardMapper.selectLatestOrders(Math.max(1, Math.min(limit, 20)));
    }
}
