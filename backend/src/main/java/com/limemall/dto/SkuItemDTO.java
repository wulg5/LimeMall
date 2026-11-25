package com.limemall.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品 SKU 子项参数（属于 ProductSaveDTO）。
 */
@Data
public class SkuItemDTO {

    /** 已存在的 SKU 传ID用于更新，新增时为空 */
    private Long id;

    /** 留空由服务层自动生成 */
    private String skuCode;

    @NotBlank(message = "请填写 SKU 规格")
    private String specs;

    @NotNull(message = "请填写 SKU 价格")
    @DecimalMin(value = "0.01", message = "SKU 价格必须大于 0")
    private BigDecimal price;

    @NotNull(message = "请填写 SKU 库存")
    @Min(value = 0, message = "SKU 库存不能为负数")
    private Integer stock;

    private String pic;
}
