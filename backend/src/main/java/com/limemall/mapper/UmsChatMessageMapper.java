package com.limemall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limemall.entity.UmsChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客服聊天消息数据访问层。
 */
@Mapper
public interface UmsChatMessageMapper extends BaseMapper<UmsChatMessage> {
}
