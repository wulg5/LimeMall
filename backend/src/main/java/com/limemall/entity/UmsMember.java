package com.limemall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 商城会员实体，对应表 ums_member。
 */
@Data
@TableName("ums_member")
public class UmsMember {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会员业务编号（对外展示用，随机不可遍历） */
    private String userNo;

    /** 会员账号 */
    private String username;

    /** 密码（BCrypt 加密存储） */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 手机号 */
    private String phone;

    /** 头像URL */
    private String avatar;

    /** 性别：0未知 1男 2女 */
    private Integer gender;

    /** 生日 */
    private LocalDate birthday;

    /** 会员等级：1普通 2银卡 3金卡 4钻石 */
    private Integer level;

    /** 积分（站内货币，可获取可消耗） */
    private Integer points;

    /** 成长值（只增不减，决定会员等级） */
    private Integer growth;

    /** 状态：1正常 0禁用 */
    private Integer status;

    /** 注册时间 */
    private LocalDateTime registerTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
