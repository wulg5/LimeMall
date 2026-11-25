package com.limemall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客服聊天消息实体，对应表 ums_chat_message。
 * from_type：1会员 2管理员；is_read 标记接收方是否已读。
 */
@Data
@TableName("ums_chat_message")
public class UmsChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会员ID（会话归属） */
    private Long memberId;

    /** 接待管理员ID（会员消息为空） */
    private Long adminId;

    /** 发送方：1会员 2管理员 */
    private Integer fromType;

    /** 消息内容（纯文本，前端按文本渲染防 XSS） */
    private String content;

    /** 已读：0否 1是 */
    private Integer isRead;

    /** 发送时间 */
    private LocalDateTime createTime;
}
