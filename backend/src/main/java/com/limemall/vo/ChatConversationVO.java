package com.limemall.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客服工作台会话条目。
 */
@Data
public class ChatConversationVO {

    private Long memberId;

    private String username;

    private String nickname;

    private String avatar;

    /** 最后一条消息内容 */
    private String lastContent;

    /** 最后一条消息时间 */
    private LocalDateTime lastTime;

    /** 未读数（会员发来且未读） */
    private Long unreadCount;
}
