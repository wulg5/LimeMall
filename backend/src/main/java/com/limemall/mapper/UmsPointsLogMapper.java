package com.limemall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limemall.entity.UmsPointsLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 积分批次账本数据访问层。
 */
@Mapper
public interface UmsPointsLogMapper extends BaseMapper<UmsPointsLog> {

    @Select("""
            SELECT IFNULL(SUM(amount), 0) FROM ums_points_log
            WHERE member_id = #{memberId} AND type = 1 AND status = 0 AND expire_time > NOW()
            """)
    int sumUsable(Long memberId);

    /** 未来 N 天内将过期、尚未消耗的积分合计（会员中心到期提醒） */
    @Select("""
            SELECT IFNULL(SUM(amount), 0) FROM ums_points_log
            WHERE member_id = #{memberId} AND type = 1 AND status = 0 AND amount > 0
              AND expire_time > NOW() AND expire_time <= DATE_ADD(NOW(), INTERVAL #{days} DAY)
            """)
    Integer sumExpiringWithin(@Param("memberId") Long memberId, @Param("days") int days);

    /** 未来 N 天内最早的过期时间 */
    @Select("""
            SELECT MIN(expire_time) FROM ums_points_log
            WHERE member_id = #{memberId} AND type = 1 AND status = 0 AND amount > 0
              AND expire_time > NOW() AND expire_time <= DATE_ADD(NOW(), INTERVAL #{days} DAY)
            """)
    LocalDateTime earliestExpireWithin(@Param("memberId") Long memberId, @Param("days") int days);
}
