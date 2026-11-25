package com.limemall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limemall.common.BusinessException;
import com.limemall.common.PageResult;
import com.limemall.entity.UmsMember;
import com.limemall.mapper.UmsMemberMapper;
import com.limemall.vo.MemberVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 会员管理服务（管理员端）：分页查询与启用/禁用。
 */
@Service
@RequiredArgsConstructor
public class MemberService {

    private final UmsMemberMapper memberMapper;

    /** 分页查询会员（账号/昵称/手机号模糊搜索，可按状态筛选） */
    public PageResult<MemberVO> page(long pageNum, long pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<UmsMember> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(UmsMember::getUsername, keyword)
                    .or().like(UmsMember::getNickname, keyword)
                    .or().like(UmsMember::getPhone, keyword));
        }
        if (status != null) {
            wrapper.eq(UmsMember::getStatus, status);
        }
        wrapper.orderByDesc(UmsMember::getId);
        Page<UmsMember> page = memberMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<MemberVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(page, records);
    }

    /** 启用/禁用会员（禁用后令牌即时失效，由 JWT 过滤器校验状态） */
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态参数不正确");
        }
        UmsMember existing = memberMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("会员不存在");
        }
        UmsMember update = new UmsMember();
        update.setId(id);
        update.setStatus(status);
        memberMapper.updateById(update);
    }

    private MemberVO toVO(UmsMember member) {
        MemberVO vo = new MemberVO();
        vo.setId(member.getId());
        vo.setUsername(member.getUsername());
        vo.setNickname(member.getNickname());
        vo.setPhone(member.getPhone());
        vo.setAvatar(member.getAvatar());
        vo.setGender(member.getGender());
        vo.setBirthday(member.getBirthday());
        vo.setLevel(member.getLevel());
        vo.setPoints(member.getPoints());
        vo.setStatus(member.getStatus());
        vo.setRegisterTime(member.getRegisterTime());
        return vo;
    }
}
