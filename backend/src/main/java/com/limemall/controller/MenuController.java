package com.limemall.controller;

import com.limemall.common.Result;
import com.limemall.service.MenuService;
import com.limemall.vo.MenuNodeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 菜单接口：为角色分配权限提供完整菜单树。
 */
@Tag(name = "后台-菜单权限")
@RestController
@RequestMapping("/admin/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/tree")
    @PreAuthorize("hasAnyAuthority('ums:role:list', 'ums:role:assign')")
    @Operation(summary = "完整菜单树（含按钮）")
    public Result<List<MenuNodeVO>> tree() {
        return Result.ok(menuService.tree());
    }
}
