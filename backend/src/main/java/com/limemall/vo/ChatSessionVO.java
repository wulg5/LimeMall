package com.limemall.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客服会话视图（工作台列表/会员端当前会话）。
 */
@Data
public class ChatSessionVO {

    private Long id;

    private Long memberId;

    private String memberNickname;

    private String memberUsername;

    private String memberAvatar;

    private Long agentId;

    private String agentNickname;

    /** 状态：0进行中 1已结束 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime assignTime;

    private LocalDateTime endTime;

    /** 最新一条消息内容（列表预览） */
    private String lastContent;

    private LocalDateTime lastMessageTime;

    /** 会员未读消息数（客服视角） */
    private Long unreadCount;
}
