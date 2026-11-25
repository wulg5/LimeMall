package com.limemall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limemall.common.BusinessException;
import com.limemall.common.PageResult;
import com.limemall.dto.BrandSaveDTO;
import com.limemall.entity.PmsBrand;
import com.limemall.entity.PmsProduct;
import com.limemall.mapper.PmsBrandMapper;
import com.limemall.mapper.PmsProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 品牌服务：分页查询、下拉选项、增删改。
 */
@Service
@RequiredArgsConstructor
public class BrandService {

    private final PmsBrandMapper brandMapper;
    private final PmsProductMapper productMapper;

    /** 分页查询品牌（名称模糊搜索） */
    public PageResult<PmsBrand> page(long pageNum, long pageSize, String keyword) {
        LambdaQueryWrapper<PmsBrand> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(PmsBrand::getName, keyword);
        }
        wrapper.orderByDesc(PmsBrand::getSort).orderByAsc(PmsBrand::getId);
        Page<PmsBrand> page = brandMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page);
    }

    /** 启用品牌下拉选项（商品编辑页使用） */
    public List<PmsBrand> options() {
        return brandMapper.selectList(new LambdaQueryWrapper<PmsBrand>()
                .eq(PmsBrand::getStatus, 1)
                .orderByDesc(PmsBrand::getSort));
    }

    public void create(BrandSaveDTO dto) {
        Long exists = brandMapper.selectCount(new LambdaQueryWrapper<PmsBrand>()
                .eq(PmsBrand::getName, dto.getName()));
        if (exists > 0) {
            throw new BusinessException("品牌名称已存在");
        }
        PmsBrand brand = new PmsBrand();
        brand.setName(dto.getName());
        brand.setFirstLetter(dto.getFirstLetter());
        brand.setLogo(dto.getLogo());
        brand.setDescription(dto.getDescription());
        brand.setSort(dto.getSort());
        brand.setStatus(dto.getStatus());
        brandMapper.insert(brand);
    }

    public void update(BrandSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("缺少品牌ID");
        }
        PmsBrand existing = brandMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BusinessException("品牌不存在");
        }
        Long exists = brandMapper.selectCount(new LambdaQueryWrapper<PmsBrand>()
                .eq(PmsBrand::getName, dto.getName())
                .ne(PmsBrand::getId, dto.getId()));
        if (exists > 0) {
            throw new BusinessException("品牌名称已存在");
        }
        existing.setName(dto.getName());
        existing.setFirstLetter(dto.getFirstLetter());
        existing.setLogo(dto.getLogo());
        existing.setDescription(dto.getDescription());
        existing.setSort(dto.getSort());
        existing.setStatus(dto.getStatus());
        brandMapper.updateById(existing);
    }

    /** 删除品牌：已被商品引用时禁止删除 */
    public void delete(Long id) {
        Long products = productMapper.selectCount(new LambdaQueryWrapper<PmsProduct>()
                .eq(PmsProduct::getBrandId, id));
        if (products > 0) {
            throw new BusinessException("该品牌下存在商品，无法删除");
        }
        brandMapper.deleteById(id);
    }
}
