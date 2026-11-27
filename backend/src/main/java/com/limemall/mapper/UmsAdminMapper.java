package com.limemall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limemall.entity.UmsAdmin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 管理员数据访问层。
 */
@Mapper
public interface UmsAdminMapper extends BaseMapper<UmsAdmin> {

    @Select("SELECT id FROM ums_admin WHERE id = #{id} AND status = 1 FOR UPDATE")
    Long lockActiveById(Long id);
}
