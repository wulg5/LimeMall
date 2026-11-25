package com.limemall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品评价实体，对应表 pms_comment。
 * 唯一约束 (order_id, product_id)：同一订单同一商品仅可评价一次。
 * 昵称与等级为评价时快照，会员后续升级不影响历史展示。
 */
@Data
@TableName("pms_comment")
public class PmsComment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品ID */
    private Long productId;

    /** 订单ID（评价资格来源） */
    private Long orderId;

    /** 会员ID */
    private Long memberId;

    /** 会员昵称（快照） */
    private String memberNickname;

    /** 会员等级（快照，用于展示徽章） */
    private Integer memberLevel;

    /** 评分 1-5 */
    private Integer rating;

    /** 评价内容（纯文本，前端按文本渲染防 XSS） */
    private String content;

    /** 评价时间 */
    private LocalDateTime createTime;
}
