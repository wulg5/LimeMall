package com.limemall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.limemall.common.BusinessException;
import com.limemall.dto.RoleSaveDTO;
import com.limemall.entity.UmsAdminRole;
import com.limemall.entity.UmsRole;
import com.limemall.entity.UmsRoleMenu;
import com.limemall.mapper.UmsAdminRoleMapper;
import com.limemall.mapper.UmsChatSessionMapper;
import com.limemall.mapper.UmsRoleMapper;
import com.limemall.mapper.UmsRoleMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色管理服务：增删改查与菜单权限分配。
 */
@Service
@RequiredArgsConstructor
public class RoleService {

    /** 超级管理员角色ID，禁止删除 */
    private static final long SUPER_ROLE_ID = 1L;

    private final UmsRoleMapper roleMapper;
    private final UmsRoleMenuMapper roleMenuMapper;
    private final UmsAdminRoleMapper adminRoleMapper;
    private final UmsChatSessionMapper chatSessionMapper;

    /** 全部角色列表（按排序降序） */
    public List<UmsRole> list() {
        return roleMapper.selectList(new LambdaQueryWrapper<UmsRole>()
                .orderByDesc(UmsRole::getSort));
    }

    /** 新增角色（可同时分配菜单） */
    @Transactional
    public void create(RoleSaveDTO dto) {
        UmsRole role = new UmsRole();
        role.setName(dto.getName());
        role.setDescription(dto.getDescription());
        role.setSort(dto.getSort());
        role.setStatus(dto.getStatus());
        roleMapper.insert(role);
        if (dto.getMenuIds() != null && !dto.getMenuIds().isEmpty()) {
            saveMenus(role.getId(), dto.getMenuIds());
        }
    }

    /** 编辑角色（menuIds 非空时同步重置菜单权限） */
    @Transactional
    public void update(RoleSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("缺少角色ID");
        }
        UmsRole existing = roleMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BusinessException("角色不存在");
        }
        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        existing.setSort(dto.getSort());
        existing.setStatus(dto.getStatus());
        roleMapper.updateById(existing);
        if (dto.getMenuIds() != null && !dto.getMenuIds().isEmpty()) {
            roleMenuMapper.delete(new LambdaQueryWrapper<UmsRoleMenu>()
                    .eq(UmsRoleMenu::getRoleId, dto.getId()));
            saveMenus(dto.getId(), dto.getMenuIds());
            chatSessionMapper.releaseIneligibleSessions(null);
        }
    }

    /** 删除角色：被管理员引用的角色不允许删除 */
    @Transactional
    public void delete(Long id) {
        if (id == SUPER_ROLE_ID) {
            throw new BusinessException("超级管理员角色不允许删除");
        }
        Long used = adminRoleMapper.selectCount(new LambdaQueryWrapper<UmsAdminRole>()
                .eq(UmsAdminRole::getRoleId, id));
        if (used > 0) {
            throw new BusinessException("该角色仍被管理员使用，请先解除绑定");
        }
        roleMapper.deleteById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<UmsRoleMenu>()
                .eq(UmsRoleMenu::getRoleId, id));
    }

    /** 查询角色已分配的菜单ID列表（分配权限回显） */
    public List<Long> getMenuIds(Long roleId) {
        return roleMenuMapper.selectList(new LambdaQueryWrapper<UmsRoleMenu>()
                        .eq(UmsRoleMenu::getRoleId, roleId))
                .stream().map(UmsRoleMenu::getMenuId).toList();
    }

    /** 为角色分配菜单权限（覆盖式） */
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        UmsRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        roleMenuMapper.delete(new LambdaQueryWrapper<UmsRoleMenu>()
                .eq(UmsRoleMenu::getRoleId, roleId));
        if (menuIds != null && !menuIds.isEmpty()) {
            saveMenus(roleId, menuIds);
        }
        chatSessionMapper.releaseIneligibleSessions(null);
    }

    private void saveMenus(Long roleId, List<Long> menuIds) {
        for (Long menuId : menuIds) {
            UmsRoleMenu relation = new UmsRoleMenu();
            relation.setRoleId(roleId);
            relation.setMenuId(menuId);
            roleMenuMapper.insert(relation);
        }
    }
}
