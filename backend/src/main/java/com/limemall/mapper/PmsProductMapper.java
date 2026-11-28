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
            SELECT p.*, COALESCE(priced.display_price, p.price) AS display_price,
                   priced.flash_price,
                   CASE WHEN priced.flash_price IS NOT NULL THEN priced.regular_price END AS flash_original_price
            FROM pms_product p
            LEFT JOIN (
                SELECT s.product_id, s.price AS regular_price, f.flash_price,
                       COALESCE(f.flash_price, s.price) AS display_price,
                       ROW_NUMBER() OVER (PARTITION BY s.product_id
                           ORDER BY COALESCE(f.flash_price, s.price), f.flash_price IS NULL, s.id) AS price_rank
                FROM pms_sku s
                LEFT JOIN sms_flash_sale f ON f.sku_id = s.id AND f.status = 1
                    AND f.start_time &lt;= #{now} AND f.end_time &gt;= #{now} AND f.flash_price &lt; s.price
                WHERE s.stock &gt; 0
            ) priced ON priced.product_id = p.id AND priced.price_rank = 1
            ${ew.customSqlSegment}
            <if test="ascending != null">
                ORDER BY COALESCE(priced.display_price, p.price)
                <choose><when test="ascending">ASC</when><otherwise>DESC</otherwise></choose>, p.id DESC
            </if>
            </script>
            """)
    IPage<PmsProduct> selectPageByDisplayPrice(IPage<PmsProduct> page,
                                            @Param(Constants.WRAPPER) Wrapper<PmsProduct> wrapper,
                                            @Param("ascending") Boolean ascending,
                                            @Param("now") LocalDateTime now);
}
