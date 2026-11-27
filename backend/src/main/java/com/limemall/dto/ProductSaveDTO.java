package com.limemall.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品新增/编辑请求参数（SPU 与其 SKU 列表）。
 * SPU 的展示价与总库存由服务层根据 SKU 列表汇总计算，无需前端传入。
 */
@Data
public class ProductSaveDTO {

    /** 编辑时必填 */
    private Long id;

    @NotBlank(message = "请输入商品名称")
    @Size(max = 200, message = "商品名称不能超过 200 字")
    private String name;

    @Size(max = 255, message = "副标题不能超过 255 字")
    private String subTitle;

    @NotNull(message = "请选择商品分类")
    private Long categoryId;

    private Long brandId;

    private String mainPic;

    private String album;

    private BigDecimal originalPrice;

    private String unit;

    private String detailHtml;

    private Integer sort;

    @NotNull(message = "请选择上架状态")
    private Integer publishStatus;

    @NotEmpty(message = "请至少添加一个 SKU")
    @Valid
    private List<SkuItemDTO> skus;

    @JsonIgnore
    private boolean brandIdProvided;

    @JsonIgnore
    private boolean originalPriceProvided;

    public void setBrandId(Long brandId) {
        this.brandId = brandId;
        this.brandIdProvided = true;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
        this.originalPriceProvided = true;
    }
}
