package com.limemall.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树节点。
 */
@Data
public class MenuNodeVO {

    private Long id;

    private Long parentId;

    private String name;

    private String path;

    private String icon;

    /** 类型：0目录 1菜单 2按钮 */
    private Integer type;

    private Integer sort;

    private List<MenuNodeVO> children = new ArrayList<>();
}
