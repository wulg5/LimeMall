package com.limemall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limemall.common.BusinessException;
import com.limemall.common.PageResult;
import com.limemall.dto.AdminSaveDTO;
import com.limemall.entity.UmsAdmin;
import com.limemall.entity.UmsAdminRole;
import com.limemall.entity.UmsRole;
import com.limemall.mapper.UmsAdminMapper;
import com.limemall.mapper.UmsAdminRoleMapper;
import com.limemall.mapper.UmsRoleMapper;
import com.limemall.vo.AdminVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 管理员管理服务：分页查询、新增、编辑、重置密码、删除（含角色绑定）。
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    /** 超级管理员账号ID，禁止删除 */
    private static final long SUPER_ADMIN_ID = 1L;
    /** 默认初始密码 */
    private static final String DEFAULT_PASSWORD = "admin123";

    private final UmsAdminMapper adminMapper;
    private final UmsAdminRoleMapper adminRoleMapper;
    private final UmsRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final com.limemall.security.CryptoService cryptoService;

    /**
     * 分页查询管理员（支持账号/昵称模糊搜索），并填充角色信息。
     */
    public PageResult<AdminVO> page(long pageNum, long pageSize, String keyword) {
        LambdaQueryWrapper<UmsAdmin> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(UmsAdmin::getUsername, keyword)
                    .or().like(UmsAdmin::getNickname, keyword));
        }
        wrapper.orderByAsc(UmsAdmin::getId);
        Page<UmsAdmin> page = adminMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<UmsAdmin> admins = page.getRecords();
        if (admins.isEmpty()) {
            return PageResult.of(page, new ArrayList<>());
        }

        // 批量查询 管理员-角色 关系与角色表，避免 N+1 查询
        List<Long> adminIds = admins.stream().map(UmsAdmin::getId).toList();
        List<UmsAdminRole> relations = adminRoleMapper.selectList(
                new LambdaQueryWrapper<UmsAdminRole>().in(UmsAdminRole::getAdminId, adminIds));
        Map<Long, List<Long>> adminRoleIds = relations.stream()
                .collect(Collectors.groupingBy(UmsAdminRole::getAdminId,
                        Collectors.mapping(UmsAdminRole::getRoleId, Collectors.toList())));
        Map<Long, UmsRole> roleMap = roleMapper.selectList(null).stream()
                .collect(Collectors.toMap(UmsRole::getId, Function.identity()));

        List<AdminVO> records = admins.stream().map(admin -> {
            AdminVO vo = new AdminVO();
            vo.setId(admin.getId());
            vo.setUsername(admin.getUsername());
            vo.setNickname(admin.getNickname());
            vo.setAvatar(admin.getAvatar());
            vo.setEmail(admin.getEmail());
            vo.setPhone(admin.getPhone());
            vo.setStatus(admin.getStatus());
            vo.setCreateTime(admin.getCreateTime());
            List<Long> roleIds = adminRoleIds.getOrDefault(admin.getId(), new ArrayList<>());
            vo.setRoleIds(roleIds);
            vo.setRoleNames(roleIds.stream()
                    .map(roleMap::get)
                    .filter(role -> role != null)
                    .map(UmsRole::getName)
                    .collect(Collectors.joining("、")));
            return vo;
        }).toList();
        return PageResult.of(page, records);
    }

    /**
     * 新增管理员：账号唯一，密码缺省为 admin123，同时绑定角色。
     */
    @Transactional
    public void create(AdminSaveDTO dto) {
        Long exists = adminMapper.selectCount(new LambdaQueryWrapper<UmsAdmin>()
                .eq(UmsAdmin::getUsername, dto.getUsername()));
        if (exists > 0) {
            throw new BusinessException("账号已存在");
        }
        UmsAdmin admin = new UmsAdmin();
        admin.setUsername(dto.getUsername());
        // 密码可选：提供时必须是 RSA 密文（解密后校验长度）；未提供用默认密码
        String rawPassword = DEFAULT_PASSWORD;
        if (StringUtils.hasText(dto.getPassword())) {
            rawPassword = cryptoService.requireDecrypted(dto.getPassword(), dto.getEncrypted());
            if (rawPassword.length() < 6 || rawPassword.length() > 32) {
                throw new BusinessException("密码长度为 6-32 位");
            }
        }
        admin.setPassword(passwordEncoder.encode(rawPassword));
        admin.setNickname(dto.getNickname());
        admin.setAvatar(dto.getAvatar());
        admin.setEmail(dto.getEmail());
        admin.setPhone(dto.getPhone());
        admin.setStatus(dto.getStatus());
        adminMapper.insert(admin);
        saveRoles(admin.getId(), dto.getRoleIds());
    }

    /**
     * 编辑管理员：账号不可修改，密码通过重置接口修改。
     */
    @Transactional
    public void update(AdminSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("缺少管理员ID");
        }
        UmsAdmin existing = adminMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BusinessException("管理员不存在");
        }
        if (!existing.getUsername().equals(dto.getUsername())) {
            Long exists = adminMapper.selectCount(new LambdaQueryWrapper<UmsAdmin>()
                    .eq(UmsAdmin::getUsername, dto.getUsername())
                    .ne(UmsAdmin::getId, dto.getId()));
            if (exists > 0) {
                throw new BusinessException("账号已存在");
            }
            // 账号允许修改（同步更新）
            existing.setUsername(dto.getUsername());
        }
        existing.setNickname(dto.getNickname());
        existing.setAvatar(dto.getAvatar());
        existing.setEmail(dto.getEmail());
        existing.setPhone(dto.getPhone());
        existing.setStatus(dto.getStatus());
        adminMapper.updateById(existing);
        adminRoleMapper.delete(new LambdaQueryWrapper<UmsAdminRole>()
                .eq(UmsAdminRole::getAdminId, dto.getId()));
        saveRoles(dto.getId(), dto.getRoleIds());
    }

    /**
     * 重置密码为默认密码 admin123。
     */
    public void resetPassword(Long id) {
        UmsAdmin existing = adminMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("管理员不存在");
        }
        UmsAdmin update = new UmsAdmin();
        update.setId(id);
        update.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        adminMapper.updateById(update);
    }

    /**
     * 删除管理员（禁止删除超级管理员与当前登录账号由控制器校验）。
     */
    @Transactional
    public void delete(Long id) {
        if (id == SUPER_ADMIN_ID) {
            throw new BusinessException("超级管理员账号不允许删除");
        }
        adminMapper.deleteById(id);
        adminRoleMapper.delete(new LambdaQueryWrapper<UmsAdminRole>()
                .eq(UmsAdminRole::getAdminId, id));
    }

    private void saveRoles(Long adminId, List<Long> roleIds) {
        if (roleIds == null) {
            return;
        }
        for (Long roleId : roleIds) {
            UmsAdminRole relation = new UmsAdminRole();
            relation.setAdminId(adminId);
            relation.setRoleId(roleId);
            adminRoleMapper.insert(relation);
        }
    }
}
