package com.limemall.vo;

import lombok.Data;

/**
 * 商城前台会员信息视图（脱敏）。
 */
@Data
public class MemberInfoVO {

    private Long id;

    /** 会员业务编号（对外展示用，不可遍历） */
    private String userNo;

    private String username;

    private String nickname;

    private String avatar;

    private String phone;

    /** 会员等级：1普通 2银卡 3金卡 4钻石 */
    private Integer level;

    /** 积分（站内货币，可兑换优惠券） */
    private Integer points;

    /** 成长值（只增不减，决定等级） */
    private Integer growth;

    /** 升级到下一等级所需成长值（钻石会员为 null） */
    private Integer nextLevelGrowth;
}
