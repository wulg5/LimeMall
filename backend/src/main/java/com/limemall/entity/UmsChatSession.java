package com.limemall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客服会话实体。
 * 每个会员同一时间仅有一个进行中会话；agent_id 为空表示待接入，接入后一对一绑定客服。
 */
@Data
@TableName("ums_chat_session")
public class UmsChatSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long memberId;

    /** 接待客服（管理员ID），NULL=待接入 */
    private Long agentId;

    /** 状态：0进行中 1已结束 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime assignTime;

    private LocalDateTime endTime;
}
