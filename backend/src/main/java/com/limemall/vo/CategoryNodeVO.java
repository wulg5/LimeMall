package com.limemall.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 商品分类树节点（两级）。
 */
@Data
public class CategoryNodeVO {

    private Long id;

    private Long parentId;

    private String name;

    /** 层级：1一级 2二级 */
    private Integer level;

    private String icon;

    private Integer sort;

    private Integer status;

    private List<CategoryNodeVO> children = new ArrayList<>();
}
