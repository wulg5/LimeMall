package com.limemall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商城公告实体。
 */
@Data
@TableName("cms_notice")
public class CmsNotice {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String content;

    private Integer sort;

    /** 状态：1显示 0隐藏 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
