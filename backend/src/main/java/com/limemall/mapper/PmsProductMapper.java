package com.limemall.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.limemall.entity.PmsProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 商品SPU数据访问层。
 * 逻辑删除由 @TableLogic 自动处理：查询自动追加 delete_status = 0。
 */
@Mapper
public interface PmsProductMapper extends BaseMapper<PmsProduct> {

    @Select("SELECT id FROM pms_product WHERE id = #{id} FOR UPDATE")
    Long lockById(Long id);

    // customSqlSegment 由 LambdaQueryWrapper 生成，搜索值通过绑定参数传入。
    @Select("""
            <script>
            SELECT p.*, active_flash.flash_min AS flash_price
            FROM pms_product p
            LEFT JOIN (
                SELECT s.product_id AS flash_product_id, MIN(f.flash_price) AS flash_min
                FROM sms_flash_sale f JOIN pms_sku s ON s.id = f.sku_id
                WHERE f.status = 1 AND f.start_time &lt;= #{now} AND f.end_time &gt;= #{now}
                GROUP BY s.product_id
            ) active_flash ON active_flash.flash_product_id = p.id
            ${ew.customSqlSegment}
            ORDER BY COALESCE(active_flash.flash_min, p.price)
            <choose><when test="ascending">ASC</when><otherwise>DESC</otherwise></choose>, p.id DESC
            </script>
            """)
    IPage<PmsProduct> selectPageByDisplayPrice(IPage<PmsProduct> page,
                                            @Param(Constants.WRAPPER) Wrapper<PmsProduct> wrapper,
                                            @Param("ascending") boolean ascending,
                                            @Param("now") LocalDateTime now);
}
