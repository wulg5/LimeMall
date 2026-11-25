package com.limemall.vo;

import lombok.Data;

/**
 * 商城前台会员信息视图（脱敏）。
 */
@Data
public class MemberInfoVO {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private String phone;

    /** 会员等级：1普通 2银卡 3金卡 4钻石 */
    private Integer level;

    private Integer points;

    /** 升级到下一等级所需积分（钻石会员为 null） */
    private Integer nextLevelPoints;
}
