package com.limemall.controller;

import com.limemall.common.BusinessException;
import com.limemall.common.PageResult;
import com.limemall.common.Result;
import com.limemall.dto.AdminSaveDTO;
import com.limemall.security.SecurityUtils;
import com.limemall.service.AdminService;
import com.limemall.vo.AdminVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员管理接口。
 */
@Tag(name = "后台-管理员管理")
@RestController
@RequestMapping("/admin/admins")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping
    @PreAuthorize("hasAuthority('ums:admin:list')")
    @Operation(summary = "分页查询管理员")
    public Result<PageResult<AdminVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize,
                                            @RequestParam(required = false) String keyword) {
        return Result.ok(adminService.page(pageNum, pageSize, keyword));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ums:admin:create')")
    @Operation(summary = "新增管理员")
    public Result<Void> create(@Valid @RequestBody AdminSaveDTO dto) {
        adminService.create(dto);
        return Result.ok();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('ums:admin:update')")
    @Operation(summary = "编辑管理员")
    public Result<Void> update(@Valid @RequestBody AdminSaveDTO dto) {
        adminService.update(dto);
        return Result.ok();
    }

    @PutMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('ums:admin:reset')")
    @Operation(summary = "重置密码为 admin123")
    public Result<Void> resetPassword(@PathVariable Long id) {
        adminService.resetPassword(id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ums:admin:delete')")
    @Operation(summary = "删除管理员")
    public Result<Void> delete(@PathVariable Long id) {
        if (id.equals(SecurityUtils.getAdminId())) {
            throw new BusinessException("不能删除当前登录账号");
        }
        adminService.delete(id);
        return Result.ok();
    }
}
