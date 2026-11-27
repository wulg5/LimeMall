package com.limemall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limemall.entity.PmsComment;
import com.limemall.vo.ProductRatingVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商品评价数据访问层。
 */
@Mapper
public interface PmsCommentMapper extends BaseMapper<PmsComment> {

    /**
     * 全部商品的评分聚合（商品数量级小，一次取回内存分发，避免动态 IN）。
     */
    @Select("""
            SELECT product_id AS productId,
                   COUNT(*) AS commentCount,
                   ROUND(AVG(rating), 1) AS avgRating
            FROM pms_comment
            WHERE hidden = 0
            GROUP BY product_id
            """)
    List<ProductRatingVO> selectAllRatings();

    /**
     * 单个商品评分聚合。
     */
    @Select("""
            SELECT COUNT(*) AS commentCount, ROUND(AVG(rating), 1) AS avgRating
            FROM pms_comment
            WHERE product_id = #{productId} AND hidden = 0
            """)
    ProductRatingVO selectRatingByProduct(@Param("productId") Long productId);
}
