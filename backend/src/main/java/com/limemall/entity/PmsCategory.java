package com.limemall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品分类实体，对应表 pms_category（两级树结构）。
 */
@Data
@TableName("pms_category")
public class PmsCategory {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父级ID（0为一级分类） */
    private Long parentId;

    /** 分类名称 */
    private String name;

    /** 层级：1一级 2二级 */
    private Integer level;

    /** 图标 */
    private String icon;

    /** 排序 */
    private Integer sort;

    /** 状态：1显示 0隐藏 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
