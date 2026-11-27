package com.limemall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limemall.entity.UmsMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 商城会员数据访问层。
 */
@Mapper
public interface UmsMemberMapper extends BaseMapper<UmsMember> {

    @Select("SELECT * FROM ums_member WHERE id = #{id} FOR UPDATE")
    UmsMember lockById(Long id);
}
