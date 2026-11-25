package com.limemall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.limemall.common.BusinessException;
import com.limemall.dto.CategorySaveDTO;
import com.limemall.entity.PmsCategory;
import com.limemall.entity.PmsProduct;
import com.limemall.mapper.PmsCategoryMapper;
import com.limemall.mapper.PmsProductMapper;
import com.limemall.vo.CategoryNodeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品分类服务：两级分类树的查询与维护。
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final PmsCategoryMapper categoryMapper;
    private final PmsProductMapper productMapper;

    /** 查询分类树（仅显示启用的分类），按排序降序 */
    public List<CategoryNodeVO> tree() {
        List<PmsCategory> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<PmsCategory>()
                        .eq(PmsCategory::getStatus, 1)
                        .orderByDesc(PmsCategory::getSort));
        Map<Long, CategoryNodeVO> nodeMap = new LinkedHashMap<>();
        for (PmsCategory category : categories) {
            nodeMap.put(category.getId(), toNode(category));
        }
        List<CategoryNodeVO> roots = new ArrayList<>();
        for (CategoryNodeVO node : nodeMap.values()) {
            CategoryNodeVO parent = nodeMap.get(node.getParentId());
            if (node.getParentId() == null || node.getParentId() == 0 || parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    /** 新增分类：支持一级/二级，最多两级 */
    public void create(CategorySaveDTO dto) {
        Integer level = resolveLevel(dto.getParentId());
        PmsCategory category = new PmsCategory();
        category.setParentId(dto.getParentId());
        category.setName(dto.getName());
        category.setLevel(level);
        category.setIcon(dto.getIcon());
        category.setSort(dto.getSort());
        category.setStatus(dto.getStatus());
        categoryMapper.insert(category);
    }

    /** 编辑分类：不支持变更上级（避免层级错乱），仅调整名称等信息 */
    public void update(CategorySaveDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("缺少分类ID");
        }
        PmsCategory existing = categoryMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BusinessException("分类不存在");
        }
        if (!existing.getParentId().equals(dto.getParentId())) {
            throw new BusinessException("不支持修改上级分类，请删除后重新创建");
        }
        existing.setName(dto.getName());
        existing.setIcon(dto.getIcon());
        existing.setSort(dto.getSort());
        existing.setStatus(dto.getStatus());
        categoryMapper.updateById(existing);
    }

    /** 删除分类：存在子分类或已被商品引用时禁止删除 */
    public void delete(Long id) {
        Long children = categoryMapper.selectCount(new LambdaQueryWrapper<PmsCategory>()
                .eq(PmsCategory::getParentId, id));
        if (children > 0) {
            throw new BusinessException("该分类下存在子分类，无法删除");
        }
        Long products = productMapper.selectCount(new LambdaQueryWrapper<PmsProduct>()
                .eq(PmsProduct::getCategoryId, id));
        if (products > 0) {
            throw new BusinessException("该分类下存在商品，无法删除");
        }
        categoryMapper.deleteById(id);
    }

    private Integer resolveLevel(Long parentId) {
        if (parentId == null || parentId == 0) {
            return 1;
        }
        PmsCategory parent = categoryMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException("上级分类不存在");
        }
        if (parent.getLevel() >= 2) {
            throw new BusinessException("商品分类最多支持两级");
        }
        return 2;
    }

    private CategoryNodeVO toNode(PmsCategory category) {
        CategoryNodeVO node = new CategoryNodeVO();
        node.setId(category.getId());
        node.setParentId(category.getParentId());
        node.setName(category.getName());
        node.setLevel(category.getLevel());
        node.setIcon(category.getIcon());
        node.setSort(category.getSort());
        node.setStatus(category.getStatus());
        return node;
    }
}
