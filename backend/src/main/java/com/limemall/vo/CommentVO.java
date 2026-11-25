package com.limemall.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价管理视图（管理端）。
 */
@Data
public class CommentVO {

    private Long id;

    private Long productId;

    private String productName;

    private Long orderId;

    private String memberNickname;

    private Integer memberLevel;

    private Integer rating;

    private String content;

    private LocalDateTime createTime;
}
