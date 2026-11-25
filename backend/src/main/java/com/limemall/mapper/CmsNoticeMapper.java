package com.limemall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limemall.entity.CmsNotice;
import org.apache.ibatis.annotations.Mapper;

/**
 * 公告数据访问层。
 */
@Mapper
public interface CmsNoticeMapper extends BaseMapper<CmsNotice> {
}
