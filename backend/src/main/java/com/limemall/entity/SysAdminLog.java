package com.limemall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台操作审计日志实体（AOP 自动记录写操作）。
 */
@Data
@TableName("sys_admin_log")
public class SysAdminLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long adminId;

    private String adminUsername;

    /** 模块（控制器名） */
    private String module;

    /** 动作（方法名） */
    private String action;

    private String method;

    private String uri;

    /** 请求参数（JSON 截断） */
    private String params;

    private String ip;

    /** 结果：1成功 0失败 */
    private Integer status;

    private String errorMsg;

    private Integer costMs;

    private LocalDateTime createTime;
}
