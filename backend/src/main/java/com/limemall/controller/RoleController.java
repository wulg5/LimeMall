package com.limemall.controller;

import com.limemall.common.Result;
import com.limemall.dto.AssignMenuDTO;
import com.limemall.dto.RoleSaveDTO;
import com.limemall.entity.UmsRole;
import com.limemall.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色权限管理接口。
 */
@Tag(name = "后台-角色权限")
@RestController
@RequestMapping("/admin/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("hasAuthority('ums:role:list')")
    @Operation(summary = "角色列表")
    public Result<List<UmsRole>> list() {
        return Result.ok(roleService.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ums:role:create')")
    @Operation(summary = "新增角色")
    public Result<Void> create(@Valid @RequestBody RoleSaveDTO dto) {
        roleService.create(dto);
        return Result.ok();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('ums:role:update')")
    @Operation(summary = "编辑角色")
    public Result<Void> update(@Valid @RequestBody RoleSaveDTO dto) {
        roleService.update(dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ums:role:delete')")
    @Operation(summary = "删除角色")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok();
    }

    @GetMapping("/{id}/menus")
    @PreAuthorize("hasAuthority('ums:role:assign')")
    @Operation(summary = "查询角色已分配的菜单ID")
    public Result<List<Long>> getMenuIds(@PathVariable Long id) {
        return Result.ok(roleService.getMenuIds(id));
    }

    @PutMapping("/{id}/menus")
    @PreAuthorize("hasAuthority('ums:role:assign')")
    @Operation(summary = "为角色分配菜单权限")
    public Result<Void> assignMenus(@PathVariable Long id, @Valid @RequestBody AssignMenuDTO dto) {
        roleService.assignMenus(id, dto.getMenuIds());
        return Result.ok();
    }
}
