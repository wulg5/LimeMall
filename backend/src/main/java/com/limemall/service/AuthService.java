package com.limemall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.limemall.common.BusinessException;
import com.limemall.common.ResultCode;
import com.limemall.dto.ChangePasswordDTO;
import com.limemall.dto.LoginDTO;
import com.limemall.entity.UmsAdmin;
import com.limemall.entity.UmsMenu;
import com.limemall.entity.UmsRole;
import com.limemall.mapper.UmsAdminMapper;
import com.limemall.mapper.UmsMenuMapper;
import com.limemall.mapper.UmsRoleMapper;
import com.limemall.security.JwtUtil;
import com.limemall.security.LoginUser;
import com.limemall.vo.AdminInfoVO;
import com.limemall.vo.LoginVO;
import com.limemall.vo.MenuNodeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证服务：登录、当前用户信息（角色/权限/菜单）、修改密码。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UmsAdminMapper adminMapper;
    private final UmsRoleMapper roleMapper;
    private final UmsMenuMapper menuMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginAttemptService loginAttemptService;
    private final com.limemall.security.CryptoService cryptoService;
    private final CaptchaService captchaService;

    /**
     * 管理员登录：校验账号密码与状态，签发 JWT 令牌。
     * 安全：连续失败 5 次锁定 10 分钟，抵御暴力破解。
     */
    public LoginVO login(LoginDTO dto) {
        String attemptKey = "admin:" + dto.getUsername();
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
        UmsAdmin admin = adminMapper.selectOne(new LambdaQueryWrapper<UmsAdmin>()
                .eq(UmsAdmin::getUsername, dto.getUsername()));
        // 前端按 RSA-OAEP 加密传输的密码在此解密（兼容未加密的直连调用）
        String rawPassword = Boolean.TRUE.equals(dto.getEncrypted())
                ? cryptoService.decrypt(dto.getPassword())
                : dto.getPassword();
        if (admin == null || !passwordEncoder.matches(rawPassword, admin.getPassword())) {
            loginAttemptService.recordFailure(attemptKey);
            throw new BusinessException("账号或密码错误");
        }
        if (admin.getStatus() != 1) {
            throw new BusinessException("账号已被禁用，请联系超级管理员");
        }
        loginAttemptService.clear(attemptKey);
        String token = jwtUtil.createToken(admin.getId(), admin.getUsername(), LoginUser.TYPE_ADMIN);
        return new LoginVO(token, admin.getUsername(), admin.getNickname());
    }

    /**
     * 获取当前登录管理员信息：基础资料 + 角色名 + 权限标识 + 菜单树。
     */
    public AdminInfoVO getInfo(Long adminId) {
        UmsAdmin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "账号不存在或已被删除");
        }
        List<UmsRole> roles = roleMapper.selectByAdminId(adminId);
        List<UmsMenu> menus = menuMapper.selectByAdminId(adminId);

        AdminInfoVO vo = new AdminInfoVO();
        vo.setId(admin.getId());
        vo.setUsername(admin.getUsername());
        vo.setNickname(admin.getNickname());
        vo.setAvatar(admin.getAvatar());
        vo.setRoles(roles.stream().map(UmsRole::getName).toList());
        vo.setPerms(menus.stream()
                .map(UmsMenu::getPerm)
                .filter(StringUtils::hasText)
                .distinct()
                .toList());
        vo.setMenus(buildMenuTree(menus.stream().filter(menu -> menu.getType() != 2).toList()));
        return vo;
    }

    /**
     * 修改当前管理员密码：校验原密码后更新。
     */
    public void changePassword(Long adminId, ChangePasswordDTO dto) {
        UmsAdmin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException("账号不存在");
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), admin.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        UmsAdmin update = new UmsAdmin();
        update.setId(adminId);
        update.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        adminMapper.updateById(update);
    }

    /**
     * 构建菜单树。menus 已按 sort 降序排列，同级按排序值展示。
     */
    private List<MenuNodeVO> buildMenuTree(List<UmsMenu> menus) {
        Map<Long, MenuNodeVO> nodeMap = new LinkedHashMap<>();
        for (UmsMenu menu : menus) {
            MenuNodeVO node = new MenuNodeVO();
            node.setId(menu.getId());
            node.setParentId(menu.getParentId());
            node.setName(menu.getName());
            node.setPath(menu.getPath());
            node.setIcon(menu.getIcon());
            node.setType(menu.getType());
            node.setSort(menu.getSort());
            nodeMap.put(menu.getId(), node);
        }
        List<MenuNodeVO> roots = new ArrayList<>();
        for (MenuNodeVO node : nodeMap.values()) {
            MenuNodeVO parent = nodeMap.get(node.getParentId());
            if (node.getParentId() == null || node.getParentId() == 0 || parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }
}
