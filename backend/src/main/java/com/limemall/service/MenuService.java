package com.limemall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.limemall.entity.UmsMenu;
import com.limemall.mapper.UmsMenuMapper;
import com.limemall.vo.MenuNodeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜单服务：提供完整菜单树（含按钮），供角色分配权限时展示。
 */
@Service
@RequiredArgsConstructor
public class MenuService {

    private final UmsMenuMapper menuMapper;

    /**
     * 查询全部菜单并按树形结构返回（含 type=2 的按钮节点）。
     */
    public List<MenuNodeVO> tree() {
        List<UmsMenu> menus = menuMapper.selectList(new LambdaQueryWrapper<UmsMenu>()
                .eq(UmsMenu::getStatus, 1)
                .orderByDesc(UmsMenu::getSort));

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
