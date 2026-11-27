package com.limemall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limemall.entity.SmsFlashSale;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 秒杀活动数据访问层。
 */
@Mapper
public interface SmsFlashSaleMapper extends BaseMapper<SmsFlashSale> {

    @Select("""
            <script>
            SELECT f.* FROM sms_flash_sale f
            JOIN pms_sku s ON s.id = f.sku_id
            WHERE f.status = 1 AND f.start_time &lt;= #{now} AND f.end_time &gt;= #{now}
              AND f.flash_price &lt; s.price AND s.stock &gt; 0
            <if test="skuIds != null">
                AND f.sku_id IN
                <foreach collection="skuIds" item="skuId" open="(" separator="," close=")">#{skuId}</foreach>
            </if>
            ORDER BY f.id DESC
            </script>
            """)
    List<SmsFlashSale> selectActiveDiscounts(@Param("now") LocalDateTime now,
                                             @Param("skuIds") Collection<Long> skuIds);
}
