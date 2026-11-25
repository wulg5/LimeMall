package com.limemall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limemall.common.BusinessException;
import com.limemall.common.PageResult;
import com.limemall.dto.FlashSaleSaveDTO;
import com.limemall.entity.PmsProduct;
import com.limemall.entity.PmsSku;
import com.limemall.entity.SmsFlashSale;
import com.limemall.mapper.PmsProductMapper;
import com.limemall.mapper.PmsSkuMapper;
import com.limemall.mapper.SmsFlashSaleMapper;
import com.limemall.vo.FlashSaleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 秒杀活动服务：后台管理 + 前台生效价格解析。
 * 秒杀价为服务端权威价：下单时由本服务解析，前端展示价仅作参考。
 */
@Service
@RequiredArgsConstructor
public class FlashSaleService {

    private final SmsFlashSaleMapper flashSaleMapper;
    private final PmsSkuMapper skuMapper;
    private final PmsProductMapper productMapper;

    /** 后台分页 */
    public PageResult<SmsFlashSale> page(long pageNum, long pageSize) {
        Page<SmsFlashSale> page = flashSaleMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SmsFlashSale>().orderByDesc(SmsFlashSale::getId));
        return PageResult.of(page, page.getRecords());
    }

    /** 新增/编辑 */
    public void save(FlashSaleSaveDTO dto) {
        PmsSku sku = skuMapper.selectById(dto.getSkuId());
        if (sku == null) {
            throw new BusinessException("SKU 不存在");
        }
        if (dto.getFlashPrice().compareTo(sku.getPrice()) >= 0) {
            throw new BusinessException("秒杀价必须低于该 SKU 的当前售价");
        }
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }
        Long conflict = flashSaleMapper.selectCount(new LambdaQueryWrapper<SmsFlashSale>()
                .eq(SmsFlashSale::getSkuId, dto.getSkuId())
                .ne(dto.getId() != null, SmsFlashSale::getId, dto.getId()));
        if (conflict > 0) {
            throw new BusinessException("该 SKU 已存在秒杀活动");
        }
        SmsFlashSale flash = new SmsFlashSale();
        flash.setId(dto.getId());
        flash.setName(dto.getName());
        flash.setSkuId(dto.getSkuId());
        flash.setFlashPrice(dto.getFlashPrice());
        flash.setStartTime(dto.getStartTime());
        flash.setEndTime(dto.getEndTime());
        flash.setStatus(dto.getStatus());
        if (dto.getId() == null) {
            flashSaleMapper.insert(flash);
        } else {
            flashSaleMapper.updateById(flash);
        }
    }

    public void delete(Long id) {
        flashSaleMapper.deleteById(id);
    }

    /** 当前生效的秒杀活动列表（含商品信息，供前台展示） */
    public List<FlashSaleVO> activeList() {
        List<SmsFlashSale> flashes = activeFlashes();
        if (flashes.isEmpty()) {
            return List.of();
        }
        Map<Long, PmsSku> skuMap = skuMapper.selectBatchIds(
                        flashes.stream().map(SmsFlashSale::getSkuId).toList())
                .stream().collect(Collectors.toMap(PmsSku::getId, Function.identity()));
        Map<Long, PmsProduct> productMap = productMapper.selectBatchIds(
                        skuMap.values().stream().map(PmsSku::getProductId).distinct().toList())
                .stream().collect(Collectors.toMap(PmsProduct::getId, Function.identity()));
        return flashes.stream().map(flash -> {
            PmsSku sku = skuMap.get(flash.getSkuId());
            if (sku == null) {
                return null;
            }
            PmsProduct product = productMap.get(sku.getProductId());
            if (product == null || product.getPublishStatus() == null || product.getPublishStatus() != 1) {
                return null;
            }
            FlashSaleVO vo = new FlashSaleVO();
            vo.setId(flash.getId());
            vo.setName(flash.getName());
            vo.setSkuId(sku.getId());
            vo.setSkuSpecs(sku.getSpecs());
            vo.setProductId(product.getId());
            vo.setProductName(product.getName());
            vo.setMainPic(product.getMainPic());
            vo.setOriginalPrice(sku.getPrice());
            vo.setFlashPrice(flash.getFlashPrice());
            vo.setEndTime(flash.getEndTime());
            return vo;
        }).filter(Objects::nonNull).toList();
    }

    /** SKU → 生效秒杀（下单价格与详情展示共用） */
    public Map<Long, SmsFlashSale> activeSkuMap(Collection<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return Map.of();
        }
        LocalDateTime now = LocalDateTime.now();
        return flashSaleMapper.selectList(new LambdaQueryWrapper<SmsFlashSale>()
                        .in(SmsFlashSale::getSkuId, skuIds)
                        .eq(SmsFlashSale::getStatus, 1)
                        .le(SmsFlashSale::getStartTime, now)
                        .ge(SmsFlashSale::getEndTime, now))
                .stream().collect(Collectors.toMap(SmsFlashSale::getSkuId, Function.identity()));
    }

    /** 商品 → 生效秒杀最低价（列表页展示） */
    public Map<Long, BigDecimal> activeMinPriceByProduct() {
        List<SmsFlashSale> flashes = activeFlashes();
        if (flashes.isEmpty()) {
            return Map.of();
        }
        Map<Long, PmsSku> skuMap = skuMapper.selectBatchIds(
                        flashes.stream().map(SmsFlashSale::getSkuId).toList())
                .stream().collect(Collectors.toMap(PmsSku::getId, Function.identity()));
        return flashes.stream()
                .filter(flash -> skuMap.containsKey(flash.getSkuId()))
                .collect(Collectors.groupingBy(
                        flash -> skuMap.get(flash.getSkuId()).getProductId(),
                        Collectors.collectingAndThen(
                                Collectors.minBy((a, b) -> a.getFlashPrice().compareTo(b.getFlashPrice())),
                                optional -> optional.map(SmsFlashSale::getFlashPrice).orElse(null))));
    }

    private List<SmsFlashSale> activeFlashes() {
        LocalDateTime now = LocalDateTime.now();
        return flashSaleMapper.selectList(new LambdaQueryWrapper<SmsFlashSale>()
                .eq(SmsFlashSale::getStatus, 1)
                .le(SmsFlashSale::getStartTime, now)
                .ge(SmsFlashSale::getEndTime, now)
                .orderByDesc(SmsFlashSale::getId));
    }
}
