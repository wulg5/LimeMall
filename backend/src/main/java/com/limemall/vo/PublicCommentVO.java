package com.limemall.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品公开评价视图（前台展示，不含订单/会员ID等内部字段）。
 */
@Data
public class PublicCommentVO {

    private Long id;

    /** 会员昵称（快照） */
    private String memberNickname;

    /** 会员等级（快照，用于展示徽章） */
    private Integer memberLevel;

    /** 评分 1-5 */
    private Integer rating;

    private String content;

    private LocalDateTime createTime;
}
