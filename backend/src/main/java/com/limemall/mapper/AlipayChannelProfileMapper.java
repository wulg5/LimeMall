package com.limemall.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 保存订单的原渠道配置，以及同一渠道轮换后的凭据。 */
@Mapper
public interface AlipayChannelProfileMapper {

    @Select("SELECT channel_config FROM alipay_channel_profile WHERE id = #{id}")
    String find(String id);

    @Insert("""
            INSERT INTO alipay_channel_profile (id, channel_config) VALUES (#{id}, #{config})
            ON DUPLICATE KEY UPDATE id = alipay_channel_profile.id
            """)
    void remember(@Param("id") String id, @Param("config") String config);

    @Insert("""
            INSERT INTO alipay_channel_profile (id, channel_config) VALUES (#{id}, #{config})
            ON DUPLICATE KEY UPDATE channel_config = #{config}
            """)
    void saveCredentials(@Param("id") String id, @Param("config") String config);
}
