package com.limemall.mapper;

import com.limemall.vo.CategorySalesVO;
import com.limemall.vo.LatestOrderVO;
import com.limemall.vo.OrderStatusVO;
import com.limemall.vo.TopProductVO;
import com.limemall.vo.TrendPointVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 数据看板统计 Mapper。
 * 统计口径：GMV 类指标均只统计已支付订单（状态 1 待发货 / 2 已发货 / 3 已完成）。
 */
@Mapper
public interface DashboardMapper {

    /** 今日订单数（不含已关闭） */
    @Select("SELECT COUNT(*) FROM oms_order WHERE status <> 4 AND create_time >= CURDATE()")
    Long countTodayOrders();

    /** 今日成交金额（已支付订单实付金额之和） */
    @Select("SELECT IFNULL(SUM(pay_amount), 0) FROM oms_order WHERE status IN (1,2,3) AND pay_time >= CURDATE() AND pay_time < CURDATE() + INTERVAL 1 DAY")
    BigDecimal sumTodayPayAmount();

    /** 今日新增会员数 */
    @Select("SELECT COUNT(*) FROM ums_member WHERE register_time >= CURDATE()")
    Long countTodayNewMembers();

    /** 在售商品数（已上架未删除） */
    @Select("SELECT COUNT(*) FROM pms_product WHERE publish_status = 1 AND delete_status = 0")
    Long countOnSaleProducts();

    /** 会员总数 */
    @Select("SELECT COUNT(*) FROM ums_member")
    Long countMembers();

    /** 待发货订单数 */
    @Select("SELECT COUNT(*) FROM oms_order WHERE status = 1")
    Long countPendingDeliver();

    /** 待审核退款数 */
    @Select("SELECT COUNT(*) FROM oms_order_return WHERE status = 0")
    Long countPendingReturns();

    /**
     * 按天统计销售趋势（近 N 天，仅已支付订单）。
     */
    @Select("""
            SELECT DATE(pay_time) AS date,
                   COUNT(*) AS orderCount,
                   IFNULL(SUM(pay_amount), 0) AS payAmount
            FROM oms_order
            WHERE status IN (1,2,3) AND pay_time >= #{start} AND pay_time < CURDATE() + INTERVAL 1 DAY
            GROUP BY DATE(pay_time)
            ORDER BY DATE(pay_time)
            """)
    List<TrendPointVO> selectTrend(@Param("start") LocalDate start);

    /**
     * 一级分类销售额排行（已支付订单）。
     */
    @Select("""
            SELECT pc.name AS name, SUM(oi.item_amount) AS amount
            FROM oms_order_item oi
            INNER JOIN oms_order o ON o.id = oi.order_id
            INNER JOIN pms_product p ON p.id = oi.product_id
            INNER JOIN pms_category c ON c.id = p.category_id
            INNER JOIN pms_category pc ON pc.id = c.parent_id
            WHERE o.status IN (1,2,3)
            GROUP BY pc.id, pc.name
            ORDER BY amount DESC
            """)
    List<CategorySalesVO> selectCategorySales();

    /**
     * 热销商品排行（按销量，已支付订单）。
     */
    @Select("""
            SELECT COALESCE(p.name, MAX(oi.product_name)) AS name,
                   SUM(oi.quantity) AS quantity,
                   SUM(oi.item_amount) AS amount
            FROM oms_order_item oi
            INNER JOIN oms_order o ON o.id = oi.order_id
            LEFT JOIN pms_product p ON p.id = oi.product_id
            WHERE o.status IN (1,2,3)
            GROUP BY oi.product_id, p.name
            ORDER BY quantity DESC, oi.product_id
            LIMIT #{limit}
            """)
    List<TopProductVO> selectTopProducts(@Param("limit") int limit);

    /**
     * 订单状态分布。
     */
    @Select("SELECT status, COUNT(*) AS count FROM oms_order GROUP BY status")
    List<OrderStatusVO> selectOrderStatusDistribution();

    /**
     * 最新订单列表。
     */
    @Select("""
            SELECT o.id, o.order_sn AS orderSn, m.nickname AS nickname,
                   o.pay_amount AS payAmount, o.status, o.create_time AS createTime
            FROM oms_order o
            LEFT JOIN ums_member m ON m.id = o.member_id
            ORDER BY o.id DESC
            LIMIT #{limit}
            """)
    List<LatestOrderVO> selectLatestOrders(@Param("limit") int limit);
}
