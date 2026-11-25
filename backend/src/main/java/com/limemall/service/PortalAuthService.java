package com.limemall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.limemall.common.BusinessException;
import com.limemall.dto.LoginDTO;
import com.limemall.dto.MemberRegisterDTO;
import com.limemall.entity.UmsMember;
import com.limemall.mapper.UmsMemberMapper;
import com.limemall.security.JwtUtil;
import com.limemall.security.LoginUser;
import com.limemall.util.MemberLevels;
import com.limemall.vo.LoginVO;
import com.limemall.vo.MemberInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 商城前台会员认证服务：注册、登录、个人信息。
 */
@Service
@RequiredArgsConstructor
public class PortalAuthService {

    private final UmsMemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginAttemptService loginAttemptService;

    /**
     * 会员注册：账号/手机号唯一，默认普通会员。
     */
    public void register(MemberRegisterDTO dto) {
        Long usernameExists = memberMapper.selectCount(new LambdaQueryWrapper<UmsMember>()
                .eq(UmsMember::getUsername, dto.getUsername()));
        if (usernameExists > 0) {
            throw new BusinessException("账号已被注册");
        }
        if (StringUtils.hasText(dto.getPhone())) {
            Long phoneExists = memberMapper.selectCount(new LambdaQueryWrapper<UmsMember>()
                    .eq(UmsMember::getPhone, dto.getPhone()));
            if (phoneExists > 0) {
                throw new BusinessException("手机号已被注册");
            }
        }
        UmsMember member = new UmsMember();
        member.setUsername(dto.getUsername());
        member.setPassword(passwordEncoder.encode(dto.getPassword()));
        member.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : dto.getUsername());
        member.setPhone(dto.getPhone());
        member.setAvatar("/img/avatar.svg");
        member.setGender(0);
        member.setLevel(1);
        member.setPoints(0);
        member.setStatus(1);
        member.setRegisterTime(LocalDateTime.now());
        memberMapper.insert(member);
    }

    /**
     * 会员登录：签发 MEMBER 类型 JWT。连续失败 5 次锁定 10 分钟。
     */
    public LoginVO login(LoginDTO dto) {
        String attemptKey = "member:" + dto.getUsername();
        loginAttemptService.checkBlocked(attemptKey);
        UmsMember member = memberMapper.selectOne(new LambdaQueryWrapper<UmsMember>()
                .eq(UmsMember::getUsername, dto.getUsername()));
        if (member == null || !passwordEncoder.matches(dto.getPassword(), member.getPassword())) {
            loginAttemptService.recordFailure(attemptKey);
            throw new BusinessException("账号或密码错误");
        }
        if (member.getStatus() != 1) {
            throw new BusinessException("账号已被禁用，请联系平台客服");
        }
        loginAttemptService.clear(attemptKey);
        String token = jwtUtil.createToken(member.getId(), member.getUsername(), LoginUser.TYPE_MEMBER);
        return new LoginVO(token, member.getUsername(), member.getNickname());
    }

    /** 当前会员信息（含升级进度） */
    public MemberInfoVO me(Long memberId) {
        UmsMember member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BusinessException("会员不存在");
        }
        MemberInfoVO vo = new MemberInfoVO();
        vo.setId(member.getId());
        vo.setUsername(member.getUsername());
        vo.setNickname(member.getNickname());
        vo.setAvatar(member.getAvatar());
        vo.setPhone(member.getPhone());
        vo.setLevel(member.getLevel());
        vo.setPoints(member.getPoints());
        vo.setNextLevelPoints(MemberLevels.nextLevelPoints(member.getPoints()));
        return vo;
    }
}
