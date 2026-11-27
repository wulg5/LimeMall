package com.limemall.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 已被订单引用的渠道配置只读，保留原配置供查单和退款。 */
@Mapper
public interface AlipayChannelProfileMapper {

    @Select("SELECT channel_config FROM alipay_channel_profile WHERE id = #{id}")
    String find(String id);

    @Insert("""
            INSERT INTO alipay_channel_profile (id, channel_config) VALUES (#{id}, #{config})
            ON DUPLICATE KEY UPDATE id = alipay_channel_profile.id
            """)
    void remember(@Param("id") String id, @Param("config") String config);
}
