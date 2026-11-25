package com.limemall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limemall.common.BusinessException;
import com.limemall.common.PageResult;
import com.limemall.dto.ProductSaveDTO;
import com.limemall.dto.SkuItemDTO;
import com.limemall.entity.PmsBrand;
import com.limemall.entity.PmsCategory;
import com.limemall.entity.PmsProduct;
import com.limemall.entity.PmsSku;
import com.limemall.mapper.PmsBrandMapper;
import com.limemall.mapper.PmsCategoryMapper;
import com.limemall.mapper.PmsProductMapper;
import com.limemall.mapper.PmsSkuMapper;
import com.limemall.vo.ProductDetailVO;
import com.limemall.vo.ProductVO;
import com.limemall.vo.ProductRatingVO;
import com.limemall.util.HtmlSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商品服务（SPU/SKU 管理）。
 *
 * 设计要点：
 * 1. SPU 的展示价取 SKU 最低价、总库存取 SKU 库存之和，写操作后统一由 recalcProductStat 重算；
 * 2. 编辑商品时按传入 SKU 列表做增/改/删同步，避免整表重建导致 SKU ID 漂移；
 * 3. 商品删除为逻辑删除（订单明细可追溯），SKU 随商品一并删除。
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final PmsProductMapper productMapper;
    private final PmsSkuMapper skuMapper;
    private final PmsBrandMapper brandMapper;
    private final PmsCategoryMapper categoryMapper;
    private final CommentService commentService;

    /**
     * 分页查询商品（管理端，默认排序）。
     */
    public PageResult<ProductVO> page(long pageNum, long pageSize, String name,
                                      Long categoryId, Long brandId, Integer publishStatus) {
        return page(pageNum, pageSize, name, categoryId, brandId, publishStatus, null);
    }

    /**
     * 分页查询商品（支持前台排序）。选择一级分类时自动包含其下所有二级分类的商品。
     *
     * @param sort 排序：sales（销量降序）/ priceAsc / priceDesc / 其他=默认（排序值+上新）
     */
    public PageResult<ProductVO> page(long pageNum, long pageSize, String name,
                                      Long categoryId, Long brandId, Integer publishStatus, String sort) {
        LambdaQueryWrapper<PmsProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(name), PmsProduct::getName, name)
                .eq(publishStatus != null, PmsProduct::getPublishStatus, publishStatus)
                .eq(brandId != null, PmsProduct::getBrandId, brandId);
        switch (sort == null ? "" : sort) {
            case "sales" -> wrapper.orderByDesc(PmsProduct::getSales);
            case "priceAsc" -> wrapper.orderByAsc(PmsProduct::getPrice);
            case "priceDesc" -> wrapper.orderByDesc(PmsProduct::getPrice);
            default -> wrapper.orderByDesc(PmsProduct::getSort).orderByDesc(PmsProduct::getId);
        }
        if (categoryId != null) {
            PmsCategory category = categoryMapper.selectById(categoryId);
            if (category != null && category.getLevel() == 1) {
                List<Long> ids = new ArrayList<>();
                ids.add(categoryId);
                categoryMapper.selectList(new LambdaQueryWrapper<PmsCategory>()
                                .eq(PmsCategory::getParentId, categoryId))
                        .forEach(child -> ids.add(child.getId()));
                wrapper.in(PmsProduct::getCategoryId, ids);
            } else {
                wrapper.eq(PmsProduct::getCategoryId, categoryId);
            }
        }
        Page<PmsProduct> page = productMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        // 批量装载品牌、分类名称与评分聚合，避免 N+1 查询
        Map<Long, String> brandNames = brandMapper.selectList(null).stream()
                .collect(Collectors.toMap(PmsBrand::getId, PmsBrand::getName));
        Map<Long, String> categoryNames = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(PmsCategory::getId, PmsCategory::getName));
        Map<Long, ProductRatingVO> ratings = commentService.allRatings();

        List<ProductVO> records = page.getRecords().stream().map(product -> {
            ProductVO vo = new ProductVO();
            BeanUtils.copyProperties(product, vo);
            vo.setBrandName(product.getBrandId() == null ? "" : brandNames.getOrDefault(product.getBrandId(), ""));
            vo.setCategoryName(categoryNames.getOrDefault(product.getCategoryId(), ""));
            ProductRatingVO rating = ratings.get(product.getId());
            if (rating != null) {
                vo.setAvgRating(rating.getAvgRating());
                vo.setCommentCount(rating.getCommentCount());
            }
            return vo;
        }).toList();
        return PageResult.of(page, records);
    }

    /**
     * 商品详情：SPU + SKU 列表。
     */
    public ProductDetailVO detail(Long id) {
        PmsProduct product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在或已删除");
        }
        ProductDetailVO vo = new ProductDetailVO();
        BeanUtils.copyProperties(product, vo);
        PmsCategory category = categoryMapper.selectById(product.getCategoryId());
        vo.setCategoryName(category == null ? "" : category.getName());
        if (product.getBrandId() != null) {
            PmsBrand brand = brandMapper.selectById(product.getBrandId());
            vo.setBrandName(brand == null ? "" : brand.getName());
        }
        List<PmsSku> skus = skuMapper.selectList(new LambdaQueryWrapper<PmsSku>()
                .eq(PmsSku::getProductId, id)
                .orderByAsc(PmsSku::getId));
        vo.setSkus(skus);
        ProductRatingVO rating = commentService.ratingOf(id);
        vo.setAvgRating(rating.getAvgRating());
        vo.setCommentCount(rating.getCommentCount());
        return vo;
    }

    /**
     * 新增商品：保存 SPU 后逐一保存 SKU，并汇总价格库存。
     */
    @Transactional
    public void create(ProductSaveDTO dto) {
        validateCategory(dto.getCategoryId());
        PmsProduct product = new PmsProduct();
        applyDto(product, dto);
        product.setSales(0);
        product.setDeleteStatus(0);
        product.setPrice(calcMinPrice(dto));
        product.setStock(calcTotalStock(dto));
        productMapper.insert(product);

        int index = 1;
        for (SkuItemDTO skuDto : dto.getSkus()) {
            insertSku(product.getId(), index++, skuDto);
        }
    }

    /**
     * 编辑商品：更新 SPU 并按传入列表同步 SKU（有 id 更新、无 id 新增、缺失删除）。
     */
    @Transactional
    public void update(ProductSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("缺少商品ID");
        }
        PmsProduct product = productMapper.selectById(dto.getId());
        if (product == null) {
            throw new BusinessException("商品不存在或已删除");
        }
        validateCategory(dto.getCategoryId());
        applyDto(product, dto);
        productMapper.updateById(product);

        List<PmsSku> existing = skuMapper.selectList(new LambdaQueryWrapper<PmsSku>()
                .eq(PmsSku::getProductId, dto.getId()));
        Set<Long> incomingIds = dto.getSkus().stream()
                .map(SkuItemDTO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        // 删除本次未提交的 SKU
        for (PmsSku sku : existing) {
            if (!incomingIds.contains(sku.getId())) {
                skuMapper.deleteById(sku.getId());
            }
        }
        int index = existing.size() + 1;
        for (SkuItemDTO skuDto : dto.getSkus()) {
            if (skuDto.getId() != null) {
                boolean belongs = existing.stream().anyMatch(sku -> sku.getId().equals(skuDto.getId()));
                if (!belongs) {
                    throw new BusinessException("存在不属于该商品的 SKU，请刷新后重试");
                }
                PmsSku update = new PmsSku();
                update.setId(skuDto.getId());
                update.setSpecs(skuDto.getSpecs());
                update.setPrice(skuDto.getPrice());
                update.setStock(skuDto.getStock());
                update.setPic(skuDto.getPic());
                if (StringUtils.hasText(skuDto.getSkuCode())) {
                    update.setSkuCode(skuDto.getSkuCode());
                }
                skuMapper.updateById(update);
            } else {
                insertSku(product.getId(), index++, skuDto);
            }
        }
        recalcProductStat(product.getId());
    }

    /**
     * 上架/下架。上架前校验至少存在一个 SKU。
     */
    public void updatePublishStatus(Long id, Integer status) {
        PmsProduct product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在或已删除");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态参数不正确");
        }
        if (status == 1) {
            Long skuCount = skuMapper.selectCount(new LambdaQueryWrapper<PmsSku>()
                    .eq(PmsSku::getProductId, id));
            if (skuCount == 0) {
                throw new BusinessException("请先为该商品配置至少一个 SKU 再上架");
            }
        }
        PmsProduct update = new PmsProduct();
        update.setId(id);
        update.setPublishStatus(status);
        productMapper.updateById(update);
    }

    /**
     * 删除商品：SPU 逻辑删除（保留历史订单关联），SKU 物理删除。
     */
    @Transactional
    public void delete(Long id) {
        PmsProduct product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在或已删除");
        }
        productMapper.deleteById(id);
        skuMapper.delete(new LambdaQueryWrapper<PmsSku>().eq(PmsSku::getProductId, id));
    }

    /**
     * 调整 SKU 库存并重算商品总库存。
     */
    public void updateSkuStock(Long skuId, Integer stock) {
        if (stock == null || stock < 0) {
            throw new BusinessException("库存不能为负数");
        }
        PmsSku sku = skuMapper.selectById(skuId);
        if (sku == null) {
            throw new BusinessException("SKU 不存在");
        }
        PmsSku update = new PmsSku();
        update.setId(skuId);
        update.setStock(stock);
        skuMapper.updateById(update);
        recalcProductStat(sku.getProductId());
    }

    // ------------------------------------------------------------------
    // 私有方法
    // ------------------------------------------------------------------

    private void validateCategory(Long categoryId) {
        PmsCategory category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException("商品分类不存在");
        }
        if (category.getLevel() != 2) {
            throw new BusinessException("请选择二级分类挂载商品");
        }
    }

    private void applyDto(PmsProduct product, ProductSaveDTO dto) {
        product.setName(dto.getName());
        product.setSubTitle(dto.getSubTitle());
        product.setCategoryId(dto.getCategoryId());
        product.setBrandId(dto.getBrandId());
        product.setMainPic(dto.getMainPic());
        product.setAlbum(dto.getAlbum());
        product.setOriginalPrice(dto.getOriginalPrice());
        product.setUnit(StringUtils.hasText(dto.getUnit()) ? dto.getUnit() : "件");
        // 富文本入库前消毒：移除 script/事件属性/javascript: 协议，防后台注入前台
        product.setDetailHtml(HtmlSanitizer.sanitize(dto.getDetailHtml()));
        product.setSort(dto.getSort() == null ? 0 : dto.getSort());
        product.setPublishStatus(dto.getPublishStatus());
    }

    private void insertSku(Long productId, int index, SkuItemDTO dto) {
        PmsSku sku = new PmsSku();
        sku.setProductId(productId);
        sku.setSkuCode(StringUtils.hasText(dto.getSkuCode())
                ? dto.getSkuCode()
                : String.format("SKU-%04d-%02d", productId, index));
        sku.setSpecs(dto.getSpecs());
        sku.setPrice(dto.getPrice());
        sku.setStock(dto.getStock());
        sku.setSales(0);
        sku.setPic(dto.getPic());
        skuMapper.insert(sku);
    }

    /**
     * 依据 SKU 表重算 SPU 的展示价与总库存。
     */
    private void recalcProductStat(Long productId) {
        List<PmsSku> skus = skuMapper.selectList(new LambdaQueryWrapper<PmsSku>()
                .eq(PmsSku::getProductId, productId));
        PmsProduct update = new PmsProduct();
        update.setId(productId);
        update.setStock(skus.stream().mapToInt(PmsSku::getStock).sum());
        update.setPrice(skus.stream()
                .map(PmsSku::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO));
        productMapper.updateById(update);
    }

    private BigDecimal calcMinPrice(ProductSaveDTO dto) {
        return dto.getSkus().stream()
                .map(SkuItemDTO::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private int calcTotalStock(ProductSaveDTO dto) {
        return dto.getSkus().stream().mapToInt(SkuItemDTO::getStock).sum();
    }
}
