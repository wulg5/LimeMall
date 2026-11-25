package com.limemall.util;

/**
 * 会员等级与积分规则（单一事实来源）。
 * 等级：1普通 2银卡 3金卡 4钻石；按累计积分自动升级。
 */
public final class MemberLevels {

    public static final int LEVEL2_POINTS = 500;
    public static final int LEVEL3_POINTS = 2000;
    public static final int LEVEL4_POINTS = 5000;

    private MemberLevels() {
    }

    /** 根据积分计算等级 */
    public static int resolveLevel(int points) {
        if (points >= LEVEL4_POINTS) {
            return 4;
        }
        if (points >= LEVEL3_POINTS) {
            return 3;
        }
        if (points >= LEVEL2_POINTS) {
            return 2;
        }
        return 1;
    }

    /** 下一等级所需积分（已是最高级返回 null） */
    public static Integer nextLevelPoints(int points) {
        if (points < LEVEL2_POINTS) {
            return LEVEL2_POINTS;
        }
        if (points < LEVEL3_POINTS) {
            return LEVEL3_POINTS;
        }
        if (points < LEVEL4_POINTS) {
            return LEVEL4_POINTS;
        }
        return null;
    }

    /** 消费积分倍率：银卡×1、金卡×2、钻石×3 */
    public static int pointMultiplier(int level) {
        return switch (level) {
            case 3 -> 2;
            case 4 -> 3;
            default -> 1;
        };
    }
}
