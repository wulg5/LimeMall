package com.limemall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limemall.entity.OmsOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Collection;

/**
 * 订单明细数据访问层。
 */
@Mapper
public interface OmsOrderItemMapper extends BaseMapper<OmsOrderItem> {

    @Select("""
            <script>
            SELECT EXISTS (
                SELECT 1 FROM oms_order_item oi
                JOIN oms_order o ON o.id = oi.order_id
                WHERE oi.sku_id IN
                <foreach collection="skuIds" item="skuId" open="(" separator="," close=")">
                    #{skuId}
                </foreach>
                AND (o.status IN (0, 1, 2) OR (o.status = 3 AND (
                    o.receive_time > #{returnCutoff} OR EXISTS (
                        SELECT 1 FROM oms_order_return r WHERE r.order_id = o.id AND r.status IN (0, 3, 5)
                    )
                )))
            )
            </script>
            """)
    boolean hasRestorableSku(@Param("skuIds") Collection<Long> skuIds,
                             @Param("returnCutoff") LocalDateTime returnCutoff);

    /**
     * 统计会员在指定时间窗口内、对某 SKU 的有效购买数量（不含已关闭订单）。
     * 用于秒杀活动的每人限购校验。
     */
    @Select("SELECT IFNULL(SUM(oi.quantity), 0) FROM oms_order_item oi "
            + "JOIN oms_order o ON o.id = oi.order_id "
            + "WHERE o.member_id = #{memberId} AND oi.sku_id = #{skuId} "
            + "AND o.status IN (0, 1, 2, 3) "
            + "AND o.create_time >= #{start} AND o.create_time <= #{end}")
    int sumPurchasedInWindow(@Param("memberId") Long memberId,
                             @Param("skuId") Long skuId,
                             @Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end);
}
