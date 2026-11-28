package com.limemall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limemall.common.BusinessException;
import com.limemall.entity.PmsBrand;
import com.limemall.entity.PmsCategory;
import com.limemall.entity.PmsProduct;
import com.limemall.mapper.PmsBrandMapper;
import com.limemall.mapper.PmsCategoryMapper;
import com.limemall.mapper.PmsProductMapper;
import com.limemall.vo.CategoryNodeVO;
import com.limemall.vo.PortalHomeVO;
import com.limemall.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商城前台服务：首页聚合数据与商品浏览。
 */
@Service
@RequiredArgsConstructor
public class PortalService {

    private final PmsProductMapper productMapper;
    private final PmsCategoryMapper categoryMapper;
    private final PmsBrandMapper brandMapper;
    private final CategoryService categoryService;

    /**
     * 首页数据：分类树 + 热销商品 + 新品上架。
     */
    public PortalHomeVO home() {
        PortalHomeVO vo = new PortalHomeVO();
        vo.setCategories(categoryService.tree());

        LocalDateTime now = LocalDateTime.now();
        List<PmsProduct> hot = productMapper.selectPageByDisplayPrice(new Page<>(1, 8, false),
                new LambdaQueryWrapper<PmsProduct>()
                        .eq(PmsProduct::getPublishStatus, 1)
                        .eq(PmsProduct::getDeleteStatus, 0)
                        .orderByDesc(PmsProduct::getSales, PmsProduct::getId), null, now).getRecords();
        List<PmsProduct> newest = productMapper.selectPageByDisplayPrice(new Page<>(1, 8, false),
                new LambdaQueryWrapper<PmsProduct>()
                        .eq(PmsProduct::getPublishStatus, 1)
                        .eq(PmsProduct::getDeleteStatus, 0)
                        .orderByDesc(PmsProduct::getId), null, now).getRecords();

        Map<Long, String> categoryNames = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(PmsCategory::getId, PmsCategory::getName));
        Map<Long, String> brandNames = brandMapper.selectList(null).stream()
                .collect(Collectors.toMap(PmsBrand::getId, PmsBrand::getName));
        vo.setHotProducts(hot.stream().map(p -> toVO(p, categoryNames, brandNames)).toList());
        vo.setNewProducts(newest.stream().map(p -> toVO(p, categoryNames, brandNames)).toList());
        return vo;
    }

    /**
     * 上架商品详情（未上架商品前台不可见）。
     */
    public void checkPublished(PmsProduct product) {
        if (product.getPublishStatus() == null || product.getPublishStatus() != 1) {
            throw new BusinessException("商品已下架");
        }
    }

    private ProductVO toVO(PmsProduct product, Map<Long, String> categoryNames, Map<Long, String> brandNames) {
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(product, vo);
        vo.setCategoryName(categoryNames.getOrDefault(product.getCategoryId(), ""));
        vo.setBrandName(product.getBrandId() == null ? "" : brandNames.getOrDefault(product.getBrandId(), ""));
        return vo;
    }
}
