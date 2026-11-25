package com.limemall.util;

/**
 * 会员等级与积分规则（单一事实来源）。
 *
 * 经济学设计（双账户模型）：
 * - **成长值**：消费 1 元 = 1 成长值，只增不减，决定会员等级 —— 积分消费不会导致掉级；
 * - **积分**：站内货币，按等级倍率获取（普通/银卡 100%、金卡 120%、钻石 150%），
 *   在积分商城按 100 积分 = 1 元面额兑换优惠券；获取与消耗比例恒定，防止通胀。
 *
 * 等级门槛（成长值）：普通 0 / 银卡 1000 / 金卡 5000 / 钻石 15000。
 */
public final class MemberLevels {

    public static final int LEVEL2_GROWTH = 1000;
    public static final int LEVEL3_GROWTH = 5000;
    public static final int LEVEL4_GROWTH = 15000;

    private MemberLevels() {
    }

    /** 根据成长值计算等级（只升不降：成长值只增不减） */
    public static int resolveLevel(int growth) {
        if (growth >= LEVEL4_GROWTH) {
            return 4;
        }
        if (growth >= LEVEL3_GROWTH) {
            return 3;
        }
        if (growth >= LEVEL2_GROWTH) {
            return 2;
        }
        return 1;
    }

    /** 下一等级所需成长值（已是最高级返回 null） */
    public static Integer nextLevelGrowth(int growth) {
        if (growth < LEVEL2_GROWTH) {
            return LEVEL2_GROWTH;
        }
        if (growth < LEVEL3_GROWTH) {
            return LEVEL3_GROWTH;
        }
        if (growth < LEVEL4_GROWTH) {
            return LEVEL4_GROWTH;
        }
        return null;
    }

    /** 积分获取百分比：普通/银卡 100%，金卡 120%，钻石 150% */
    public static int pointPercent(int level) {
        return switch (level) {
            case 3 -> 120;
            case 4 -> 150;
            default -> 100;
        };
    }

    /** 免运费权益：金卡及以上无门槛免运费 */
    public static boolean freeShipping(int level) {
        return level >= 3;
    }
}
