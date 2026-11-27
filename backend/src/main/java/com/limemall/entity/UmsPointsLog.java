package com.limemall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 积分批次账本实体。
 * type=1 获取批次（amount 为批次剩余，expire_time 到期后由任务核销）；
 * type=2 消费流水，type=3 过期流水，type=4 退款撤回流水。
 */
@Data
@TableName("ums_points_log")
public class UmsPointsLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long memberId;

    /** 批次剩余积分（获取为正；流水记录为对应变动量） */
    private Integer amount;

    /** 获取批次的原始入账积分。旧记录缺少原始值时为空。 */
    private Integer earnedAmount;

    /** 类型：1获取批次 2消费流水 3过期流水 4退款撤回流水 */
    private Integer type;

    private String remark;

    /** 批次过期时间（仅获取批次） */
    private LocalDateTime expireTime;

    /** 批次状态：0有效 1已过期/已核销 2退款已撤回 */
    private Integer status;

    private LocalDateTime createTime;
}
