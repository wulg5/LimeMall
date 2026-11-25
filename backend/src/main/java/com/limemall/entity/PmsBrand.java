package com.limemall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品品牌实体，对应表 pms_brand。
 */
@Data
@TableName("pms_brand")
public class PmsBrand {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 品牌名称 */
    private String name;

    /** 首字母 */
    private String firstLetter;

    /** 品牌LOGO */
    private String logo;

    /** 品牌介绍 */
    private String description;

    /** 排序 */
    private Integer sort;

    /** 状态：1显示 0隐藏 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
