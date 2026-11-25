package com.limemall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limemall.common.BusinessException;
import com.limemall.common.PageResult;
import com.limemall.dto.DeliverDTO;
import com.limemall.dto.OrderCreateDTO;
import com.limemall.entity.OmsOrder;
import com.limemall.entity.OmsOrderItem;
import com.limemall.entity.OmsOrderReturn;
import com.limemall.entity.PmsProduct;
import com.limemall.entity.PmsSku;
import com.limemall.entity.SmsCoupon;
import com.limemall.entity.SmsCouponMember;
import com.limemall.entity.UmsMember;
import com.limemall.mapper.OmsOrderItemMapper;
import com.limemall.mapper.OmsOrderMapper;
import com.limemall.mapper.OmsOrderReturnMapper;
import com.limemall.mapper.PmsProductMapper;
import com.limemall.mapper.PmsSkuMapper;
import com.limemall.mapper.SmsCouponMapper;
import com.limemall.mapper.SmsCouponMemberMapper;
import com.limemall.mapper.UmsMemberMapper;
import com.limemall.vo.OrderDetailVO;
import com.limemall.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单服务。
 *
 * 覆盖两端能力：
 * - 管理员端：分页查询、详情、发货、关闭订单；
 * - 商城前台：下单（库存条件扣减防超卖 + 优惠券核销）、模拟支付、取消、确认收货、我的订单。
 *
 * 订单状态机：0待付款 → 1待发货 → 2已发货 → 3已完成；0/1 → 4已关闭。
 * 关闭/退款时在同一事务内回补 SKU/SPU 库存、回退销量并恢复优惠券。
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    public static final int STATUS_UNPAID = 0;
    public static final int STATUS_UNSHIPPED = 1;
    public static final int STATUS_SHIPPED = 2;
    public static final int STATUS_COMPLETED = 3;
    public static final int STATUS_CLOSED = 4;

    /** 包邮门槛 */
    private static final BigDecimal FREE_FREIGHT_THRESHOLD = new BigDecimal("99");
    /** 未达包邮门槛的运费 */
    private static final BigDecimal FREIGHT_AMOUNT = new BigDecimal("10.00");

    private final OmsOrderMapper orderMapper;
    private final OmsOrderItemMapper itemMapper;
    private final OmsOrderReturnMapper returnMapper;
    private final UmsMemberMapper memberMapper;
    private final PmsSkuMapper skuMapper;
    private final PmsProductMapper productMapper;
    private final SmsCouponMapper couponMapper;
    private final SmsCouponMemberMapper couponMemberMapper;

    // ==================================================================
    // 管理员端
    // ==================================================================

    /**
     * 分页查询订单（订单号模糊、状态、下单日期区间筛选）。
     */
    public PageResult<OrderVO> page(long pageNum, long pageSize, String orderSn,
                                    Integer status, LocalDate beginDate, LocalDate endDate) {
        LambdaQueryWrapper<OmsOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(orderSn), OmsOrder::getOrderSn, orderSn)
                .eq(status != null, OmsOrder::getStatus, status)
                .ge(beginDate != null, OmsOrder::getCreateTime,
                        beginDate == null ? null : beginDate.atStartOfDay())
                .le(endDate != null, OmsOrder::getCreateTime,
                        endDate == null ? null : endDate.atTime(23, 59, 59))
                .orderByDesc(OmsOrder::getId);
        Page<OmsOrder> page = orderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page, toOrderVOs(page.getRecords()));
    }

    /**
     * 订单详情（管理员视角，含退款申请）。
     */
    public OrderDetailVO detail(Long id) {
        return toDetailVO(getOrder(id));
    }

    /**
     * 订单发货：仅「待发货」状态可操作。
     */
    @Transactional
    public void deliver(Long id, DeliverDTO dto) {
        OmsOrder order = getOrder(id);
        if (order.getStatus() != STATUS_UNSHIPPED) {
            throw new BusinessException("当前订单状态不允许发货（仅待发货订单可发货）");
        }
        OmsOrder update = new OmsOrder();
        update.setId(id);
        update.setStatus(STATUS_SHIPPED);
        update.setLogisticsCompany(dto.getLogisticsCompany());
        update.setLogisticsSn(dto.getLogisticsSn());
        update.setDeliveryTime(LocalDateTime.now());
        orderMapper.updateById(update);
    }

    /**
     * 关闭订单（管理员）：仅「待付款/待发货」可关闭，回补库存与优惠券。
     */
    @Transactional
    public void close(Long id) {
        OmsOrder order = getOrder(id);
        if (order.getStatus() != STATUS_UNPAID && order.getStatus() != STATUS_UNSHIPPED) {
            throw new BusinessException("当前订单状态不允许关闭（仅待付款/待发货订单可关闭）");
        }
        restoreStockAndCoupon(order);
        OmsOrder update = new OmsOrder();
        update.setId(id);
        update.setStatus(STATUS_CLOSED);
        update.setCloseTime(LocalDateTime.now());
        orderMapper.updateById(update);
    }

    // ==================================================================
    // 商城前台
    // ==================================================================

    /**
     * 会员下单。
     *
     * 核心流程：
     * 1. 合并重复 SKU，逐项校验商品上架状态；
     * 2. 条件更新扣减库存（stock >= quantity），影响行数为 0 即库存不足，事务回滚——防超卖；
     * 3. 校验并核销优惠券（归属、状态、有效期、门槛）；
     * 4. 生成订单与明细快照，返回订单详情。
     */
    @Transactional
    public OrderDetailVO createOrder(Long memberId, OrderCreateDTO dto) {
        // 合并相同 SKU 的数量
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (OrderCreateDTO.Item item : dto.getItems()) {
            quantities.merge(item.getSkuId(), item.getQuantity(), Integer::sum);
        }

        List<OmsOrderItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            PmsSku sku = skuMapper.selectById(entry.getKey());
            if (sku == null) {
                throw new BusinessException("商品不存在或已下架");
            }
            PmsProduct product = productMapper.selectById(sku.getProductId());
            if (product == null || product.getPublishStatus() == null || product.getPublishStatus() != 1) {
                throw new BusinessException("商品已下架");
            }
            int quantity = entry.getValue();
            // 条件扣减：stock >= quantity 才执行，杜绝超卖
            int affected = skuMapper.update(null, new LambdaUpdateWrapper<PmsSku>()
                    .setSql("stock = stock - " + quantity)
                    .eq(PmsSku::getId, sku.getId())
                    .ge(PmsSku::getStock, quantity));
            if (affected == 0) {
                throw new BusinessException("「" + product.getName() + "」库存不足");
            }
            productMapper.update(null, new LambdaUpdateWrapper<PmsProduct>()
                    .setSql("stock = GREATEST(stock - " + quantity + ", 0)")
                    .eq(PmsProduct::getId, product.getId()));

            OmsOrderItem item = new OmsOrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setProductPic(product.getMainPic());
            item.setSkuId(sku.getId());
            item.setSkuSpecs(sku.getSpecs());
            item.setPrice(sku.getPrice());
            item.setQuantity(quantity);
            item.setItemAmount(sku.getPrice().multiply(BigDecimal.valueOf(quantity))
                    .setScale(2, RoundingMode.HALF_UP));
            items.add(item);
            totalAmount = totalAmount.add(item.getItemAmount());
        }
        totalAmount = totalAmount.setScale(2, RoundingMode.HALF_UP);

        // 运费规则：满 99 包邮
        BigDecimal freightAmount = totalAmount.compareTo(FREE_FREIGHT_THRESHOLD) < 0
                ? FREIGHT_AMOUNT : BigDecimal.ZERO;

        // 优惠券校验与抵扣计算
        BigDecimal couponAmount = BigDecimal.ZERO;
        SmsCouponMember usedCouponMember = null;
        SmsCoupon usedCoupon = null;
        if (dto.getCouponMemberId() != null) {
            usedCouponMember = couponMemberMapper.selectById(dto.getCouponMemberId());
            if (usedCouponMember == null || !usedCouponMember.getMemberId().equals(memberId)
                    || usedCouponMember.getStatus() != 0) {
                throw new BusinessException("优惠券不可用");
            }
            usedCoupon = couponMapper.selectById(usedCouponMember.getCouponId());
            LocalDateTime now = LocalDateTime.now();
            if (usedCoupon == null || usedCoupon.getStatus() != 1
                    || now.isBefore(usedCoupon.getStartTime()) || now.isAfter(usedCoupon.getEndTime())) {
                throw new BusinessException("优惠券已失效");
            }
            if (totalAmount.compareTo(usedCoupon.getMinAmount()) < 0) {
                throw new BusinessException("订单金额未达到优惠券使用门槛");
            }
            if (usedCoupon.getType() == 1) {
                couponAmount = usedCoupon.getAmount();
            } else {
                couponAmount = totalAmount
                        .multiply(BigDecimal.TEN.subtract(usedCoupon.getAmount()))
                        .divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP);
            }
            if (couponAmount.compareTo(totalAmount) > 0) {
                couponAmount = totalAmount;
            }
        }

        OmsOrder order = new OmsOrder();
        order.setOrderSn(generateOrderSn());
        order.setMemberId(memberId);
        order.setTotalAmount(totalAmount);
        order.setCouponAmount(couponAmount);
        order.setFreightAmount(freightAmount);
        order.setPayAmount(totalAmount.subtract(couponAmount).add(freightAmount));
        order.setCouponId(usedCoupon == null ? null : usedCoupon.getId());
        order.setStatus(STATUS_UNPAID);
        order.setReceiverName(dto.getReceiverName());
        order.setReceiverPhone(dto.getReceiverPhone());
        order.setReceiverAddress(dto.getReceiverAddress());
        order.setNote(dto.getNote());
        orderMapper.insert(order);

        for (OmsOrderItem item : items) {
            item.setOrderId(order.getId());
            itemMapper.insert(item);
        }

        // 核销优惠券（锁定到本订单，订单取消/退款时自动恢复）
        if (usedCouponMember != null) {
            couponMemberMapper.update(null, new LambdaUpdateWrapper<SmsCouponMember>()
                    .set(SmsCouponMember::getStatus, 1)
                    .set(SmsCouponMember::getOrderId, order.getId())
                    .set(SmsCouponMember::getUseTime, LocalDateTime.now())
                    .eq(SmsCouponMember::getId, usedCouponMember.getId()));
            couponMapper.update(null, new LambdaUpdateWrapper<SmsCoupon>()
                    .setSql("used_count = used_count + 1")
                    .eq(SmsCoupon::getId, usedCoupon.getId()));
        }
        return toDetailVO(orderMapper.selectById(order.getId()));
    }

    /**
     * 模拟支付：待付款 → 待发货，累计商品销量。
     */
    @Transactional
    public void pay(Long memberId, Long id) {
        OmsOrder order = getMemberOrder(memberId, id);
        if (order.getStatus() != STATUS_UNPAID) {
            throw new BusinessException("当前订单状态不可支付");
        }
        OmsOrder update = new OmsOrder();
        update.setId(id);
        update.setStatus(STATUS_UNSHIPPED);
        update.setPayTime(LocalDateTime.now());
        orderMapper.updateById(update);
        // 支付成功累计销量
        for (OmsOrderItem item : listItems(id)) {
            skuMapper.update(null, new LambdaUpdateWrapper<PmsSku>()
                    .setSql("sales = sales + " + item.getQuantity())
                    .eq(PmsSku::getId, item.getSkuId()));
            productMapper.update(null, new LambdaUpdateWrapper<PmsProduct>()
                    .setSql("sales = sales + " + item.getQuantity())
                    .eq(PmsProduct::getId, item.getProductId()));
        }
    }

    /**
     * 会员取消订单（仅待付款），回补库存与优惠券。
     */
    @Transactional
    public void cancel(Long memberId, Long id) {
        OmsOrder order = getMemberOrder(memberId, id);
        if (order.getStatus() != STATUS_UNPAID) {
            throw new BusinessException("仅待付款订单可以取消，已支付订单请申请退款");
        }
        restoreStockAndCoupon(order);
        OmsOrder update = new OmsOrder();
        update.setId(id);
        update.setStatus(STATUS_CLOSED);
        update.setCloseTime(LocalDateTime.now());
        orderMapper.updateById(update);
    }

    /**
     * 确认收货：已发货 → 已完成。
     */
    @Transactional
    public void receive(Long memberId, Long id) {
        OmsOrder order = getMemberOrder(memberId, id);
        if (order.getStatus() != STATUS_SHIPPED) {
            throw new BusinessException("当前订单状态不可确认收货");
        }
        OmsOrder update = new OmsOrder();
        update.setId(id);
        update.setStatus(STATUS_COMPLETED);
        update.setReceiveTime(LocalDateTime.now());
        orderMapper.updateById(update);
    }

    /**
     * 我的订单分页。
     */
    public PageResult<OrderVO> pageMine(Long memberId, long pageNum, long pageSize, Integer status) {
        LambdaQueryWrapper<OmsOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OmsOrder::getMemberId, memberId)
                .eq(status != null, OmsOrder::getStatus, status)
                .orderByDesc(OmsOrder::getId);
        Page<OmsOrder> page = orderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page, toOrderVOs(page.getRecords()));
    }

    /**
     * 我的订单详情（校验归属）。
     */
    public OrderDetailVO detailMine(Long memberId, Long id) {
        return toDetailVO(getMemberOrder(memberId, id));
    }

    // ==================================================================
    // 共用逻辑
    // ==================================================================

    /**
     * 回补库存与销量、恢复优惠券（关闭订单 / 退款通过共用）。
     */
    public void restoreStockAndCoupon(OmsOrder order) {
        List<OmsOrderItem> items = listItems(order.getId());
        boolean paid = order.getStatus() != null && order.getStatus() >= STATUS_UNSHIPPED;
        for (OmsOrderItem item : items) {
            int quantity = item.getQuantity();
            skuMapper.update(null, new LambdaUpdateWrapper<PmsSku>()
                    .setSql("stock = stock + " + quantity)
                    .eq(PmsSku::getId, item.getSkuId()));
            productMapper.update(null, new LambdaUpdateWrapper<PmsProduct>()
                    .setSql("stock = stock + " + quantity)
                    .eq(PmsProduct::getId, item.getProductId()));
            if (paid) {
                skuMapper.update(null, new LambdaUpdateWrapper<PmsSku>()
                        .setSql("sales = GREATEST(sales - " + quantity + ", 0)")
                        .eq(PmsSku::getId, item.getSkuId()));
                productMapper.update(null, new LambdaUpdateWrapper<PmsProduct>()
                        .setSql("sales = GREATEST(sales - " + quantity + ", 0)")
                        .eq(PmsProduct::getId, item.getProductId()));
            }
        }
        if (order.getCouponId() != null) {
            SmsCouponMember couponMember = couponMemberMapper.selectOne(
                    new LambdaQueryWrapper<SmsCouponMember>()
                            .eq(SmsCouponMember::getOrderId, order.getId())
                            .eq(SmsCouponMember::getStatus, 1)
                            .last("LIMIT 1"));
            if (couponMember != null) {
                couponMemberMapper.update(null, new LambdaUpdateWrapper<SmsCouponMember>()
                        .set(SmsCouponMember::getStatus, 0)
                        .set(SmsCouponMember::getUseTime, null)
                        .set(SmsCouponMember::getOrderId, null)
                        .eq(SmsCouponMember::getId, couponMember.getId()));
                couponMapper.update(null, new LambdaUpdateWrapper<SmsCoupon>()
                        .setSql("used_count = GREATEST(used_count - 1, 0)")
                        .eq(SmsCoupon::getId, order.getCouponId()));
            }
        }
    }

    private OmsOrder getOrder(Long id) {
        OmsOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    /** 查询会员本人的订单，非本人订单一律视为不存在（防越权） */
    private OmsOrder getMemberOrder(Long memberId, Long id) {
        OmsOrder order = orderMapper.selectById(id);
        if (order == null || !order.getMemberId().equals(memberId)) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    private List<OmsOrderItem> listItems(Long orderId) {
        return itemMapper.selectList(new LambdaQueryWrapper<OmsOrderItem>()
                .eq(OmsOrderItem::getOrderId, orderId)
                .orderByAsc(OmsOrderItem::getId));
    }

    /** 订单号：LM + 时间戳 + 4 位随机数 */
    private String generateOrderSn() {
        return "LM" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", new Random().nextInt(10000));
    }

    /**
     * 批量构建订单列表 VO（会员昵称、商品摘要等）。
     */
    private List<OrderVO> toOrderVOs(List<OmsOrder> orders) {
        if (orders.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> orderIds = orders.stream().map(OmsOrder::getId).toList();
        Map<Long, List<OmsOrderItem>> itemsMap = itemMapper.selectList(
                        new LambdaQueryWrapper<OmsOrderItem>().in(OmsOrderItem::getOrderId, orderIds))
                .stream().collect(Collectors.groupingBy(OmsOrderItem::getOrderId));
        Map<Long, UmsMember> memberMap = memberMapper.selectBatchIds(
                        orders.stream().map(OmsOrder::getMemberId).distinct().toList())
                .stream().collect(Collectors.toMap(UmsMember::getId, Function.identity()));

        return orders.stream().map(order -> {
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(order, vo);
            UmsMember member = memberMap.get(order.getMemberId());
            vo.setMemberNickname(member == null ? "" : member.getNickname());
            List<OmsOrderItem> items = itemsMap.getOrDefault(order.getId(), List.of());
            vo.setItemCount(items.stream().mapToInt(OmsOrderItem::getQuantity).sum());
            if (!items.isEmpty()) {
                vo.setFirstItemName(items.get(0).getProductName());
                vo.setFirstItemPic(items.get(0).getProductPic());
            }
            return vo;
        }).toList();
    }

    /**
     * 构建订单详情 VO（主表 + 明细 + 会员 + 最新退款申请）。
     */
    private OrderDetailVO toDetailVO(OmsOrder order) {
        OrderDetailVO vo = new OrderDetailVO();
        BeanUtils.copyProperties(order, vo);
        UmsMember member = memberMapper.selectById(order.getMemberId());
        if (member != null) {
            vo.setMemberUsername(member.getUsername());
            vo.setMemberNickname(member.getNickname());
        }
        vo.setItems(listItems(order.getId()));
        vo.setReturnApply(returnMapper.selectOne(new LambdaQueryWrapper<OmsOrderReturn>()
                .eq(OmsOrderReturn::getOrderId, order.getId())
                .orderByDesc(OmsOrderReturn::getId)
                .last("LIMIT 1")));
        return vo;
    }
}
