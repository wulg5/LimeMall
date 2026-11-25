package com.limemall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统参数配置实体（后台可视化维护，如支付宝支付参数）。
 */
@Data
@TableName("sys_config")
public class SysConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 配置键（唯一） */
    private String cfgKey;

    /** 配置值 */
    private String cfgValue;

    private String remark;

    private LocalDateTime updateTime;
}
