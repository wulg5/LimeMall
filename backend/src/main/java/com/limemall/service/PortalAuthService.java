package com.limemall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.limemall.common.BusinessException;
import com.limemall.dto.ChangePasswordDTO;
import com.limemall.dto.LoginDTO;
import com.limemall.dto.MemberProfileDTO;
import com.limemall.dto.MemberRegisterDTO;
import com.limemall.entity.UmsMember;
import com.limemall.mapper.UmsMemberMapper;
import com.limemall.common.Strings;
import com.limemall.mapper.UmsPointsLogMapper;
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
    private final UmsPointsLogMapper pointsLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginAttemptService loginAttemptService;
    private final com.limemall.security.CryptoService cryptoService;
    private final CaptchaService captchaService;

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
        member.setUserNo(generateUserNo());
        member.setUsername(dto.getUsername());
        // 强制加密通道：注册密码必须为 RSA 密文；明文长度在解密后校验
        String rawPassword = cryptoService.requireDecrypted(dto.getPassword(), dto.getEncrypted());
        if (rawPassword.length() < 6 || rawPassword.length() > 32) {
            throw new BusinessException("密码长度为 6-32 位");
        }
        member.setPassword(passwordEncoder.encode(rawPassword));
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

    /** 生成不可遍历的会员业务编号（U + 10 位随机大写十六进制，唯一） */
    private String generateUserNo() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String userNo = "U" + java.util.UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 10).toUpperCase();
            Long exists = memberMapper.selectCount(new LambdaQueryWrapper<UmsMember>()
                    .eq(UmsMember::getUserNo, userNo));
            if (exists == 0) {
                return userNo;
            }
        }
        throw new BusinessException("生成会员编号失败，请重试");
    }

    /**
     * 会员登录：签发 MEMBER 类型 JWT。连续失败 5 次锁定 10 分钟。
     */
    public LoginVO login(LoginDTO dto) {
        String attemptKey = "member:" + dto.getUsername();
        loginAttemptService.checkBlocked(attemptKey);
        // 验证码门槛：同一账号连续失败 2 次后，登录必须携带有效图形验证码（防脚本爆破）
        if (loginAttemptService.failureCount(attemptKey) >= 2) {
            if (dto.getCaptchaId() == null || dto.getCaptchaId().isBlank()
                    || dto.getCaptchaCode() == null || dto.getCaptchaCode().isBlank()) {
                throw new BusinessException("请输入图形验证码");
            }
            if (!captchaService.verify(dto.getCaptchaId(), dto.getCaptchaCode())) {
                throw new BusinessException("验证码错误或已过期，请刷新重试");
            }
        }
        UmsMember member = memberMapper.selectOne(new LambdaQueryWrapper<UmsMember>()
                .eq(UmsMember::getUsername, dto.getUsername()));
        // 强制加密通道：拒绝明文密码（encrypted != true 直接报错）
        String rawPassword = cryptoService.requireDecrypted(dto.getPassword(), dto.getEncrypted());
        if (member == null || !passwordEncoder.matches(rawPassword, member.getPassword())) {
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

    /** 会员编辑个人资料（昵称/性别/生日/手机号改绑，手机号做唯一性校验） */
    @org.springframework.transaction.annotation.Transactional
    public void updateProfile(Long memberId, MemberProfileDTO dto) {
        UmsMember member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BusinessException("会员不存在");
        }
        String phone = Strings.blankToNull(dto.getPhone());
        if (phone != null) {
            Long exists = memberMapper.selectCount(new LambdaQueryWrapper<UmsMember>()
                    .eq(UmsMember::getPhone, phone)
                    .ne(UmsMember::getId, memberId));
            if (exists > 0) {
                throw new BusinessException("该手机号已被其他账号绑定");
            }
        }
        UmsMember update = new UmsMember();
        update.setId(memberId);
        if (StringUtils.hasText(dto.getNickname())) {
            update.setNickname(dto.getNickname().trim());
        }
        update.setPhone(phone);
        if (dto.getGender() != null) {
            update.setGender(dto.getGender());
        }
        if (dto.getBirthday() != null) {
            update.setBirthday(dto.getBirthday());
        }
        memberMapper.updateById(update);
    }

    /** 会员修改密码（新旧密码均强制加密传输） */
    public void changePassword(Long memberId, ChangePasswordDTO dto) {
        UmsMember member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BusinessException("会员不存在");
        }
        String oldRaw = cryptoService.requireDecrypted(dto.getOldPassword(), dto.getEncrypted());
        String newRaw = cryptoService.requireDecrypted(dto.getNewPassword(), dto.getEncrypted());
        if (newRaw.length() < 6 || newRaw.length() > 32) {
            throw new BusinessException("新密码长度为 6-32 位");
        }
        if (!passwordEncoder.matches(oldRaw, member.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        UmsMember update = new UmsMember();
        update.setId(memberId);
        update.setPassword(passwordEncoder.encode(newRaw));
        memberMapper.updateById(update);
    }

    /** 当前会员信息（含升级进度） */
    public MemberInfoVO me(Long memberId) {
        UmsMember member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BusinessException("会员不存在");
        }
        MemberInfoVO vo = new MemberInfoVO();
        vo.setId(member.getId());
        vo.setUserNo(member.getUserNo());
        vo.setUsername(member.getUsername());
        vo.setNickname(member.getNickname());
        vo.setAvatar(member.getAvatar());
        vo.setPhone(member.getPhone());
        vo.setGender(member.getGender());
        vo.setBirthday(member.getBirthday());
        vo.setLevel(member.getLevel());
        vo.setPoints(Math.min(member.getPoints(), pointsLogMapper.sumUsable(member.getId())));
        int growth = member.getGrowth() == null ? 0 : member.getGrowth();
        vo.setGrowth(growth);
        vo.setNextLevelGrowth(MemberLevels.nextLevelGrowth(growth));
        // 未来 30 天内将过期的积分提醒（积分 12 个月滚动过期机制的可见性）
        Integer expiring = pointsLogMapper.sumExpiringWithin(memberId, 30);
        vo.setExpiringPoints(expiring == null ? 0 : expiring);
        if (expiring != null && expiring > 0) {
            java.time.LocalDateTime earliest = pointsLogMapper.earliestExpireWithin(memberId, 30);
            vo.setExpiringDate(earliest == null ? null : earliest.toLocalDate());
        }
        return vo;
    }
}
