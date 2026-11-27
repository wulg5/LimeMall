package com.limemall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limemall.common.BusinessException;
import com.limemall.common.PageResult;
import com.limemall.dto.DeliverDTO;
import com.limemall.dto.OrderCreateDTO;
import com.limemall.dto.OrderQuoteDTO;
import com.limemall.entity.OmsOrder;
import com.limemall.entity.OmsOrderItem;
import com.limemall.entity.OmsOrderReturn;
import com.limemall.entity.PmsComment;
import com.limemall.entity.PmsProduct;
import com.limemall.entity.PmsSku;
import com.limemall.entity.SmsCoupon;
import com.limemall.entity.SmsCouponMember;
import com.limemall.entity.SmsFlashSale;
import com.limemall.entity.UmsMember;
import com.limemall.mapper.OmsOrderItemMapper;
import com.limemall.mapper.OmsOrderMapper;
import com.limemall.mapper.OmsOrderReturnMapper;
import com.limemall.mapper.PmsCommentMapper;
import com.limemall.mapper.PmsProductMapper;
import com.limemall.mapper.PmsSkuMapper;
import com.limemall.mapper.SmsCouponMapper;
import com.limemall.mapper.SmsCouponMemberMapper;
import com.limemall.mapper.UmsMemberMapper;
import com.limemall.vo.OrderDetailVO;
import com.limemall.vo.OrderQuoteVO;
import com.limemall.vo.OrderVO;
import com.limemall.util.MemberLevels;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单服务。
 *
 * 覆盖两端能力：
 * - 管理员端：分页查询、详情、发货、关闭订单；
 * - 商城前台：下单（库存条件扣减防超卖 + 优惠券核销）、发起支付宝支付、取消、确认收货、我的订单。
 *
 * 订单状态机：0待付款 → 1待发货 → 2已发货 → 3已完成；未付款关单或已退款后进入 4已关闭。
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
    static final int RETURN_DAYS = 7;

    /** 包邮门槛 */
    private static final BigDecimal FREE_FREIGHT_THRESHOLD = new BigDecimal("99");
    /** 未达包邮门槛的运费 */
    private static final BigDecimal FREIGHT_AMOUNT = new BigDecimal("10.00");

    private final OmsOrderMapper orderMapper;
    private final OmsOrderItemMapper itemMapper;
    private final OmsOrderReturnMapper returnMapper;
    private final PmsCommentMapper commentMapper;
    private final UmsMemberMapper memberMapper;
    private final PmsSkuMapper skuMapper;
    private final PmsProductMapper productMapper;
    private final SmsCouponMapper couponMapper;
    private final SmsCouponMemberMapper couponMemberMapper;
    private final FlashSaleService flashSaleService;
    private final PointsLedgerService pointsLedgerService;
    private final AlipayService alipayService;

    @Value("${limemall.order.pay-timeout-minutes:30}")
    private int payTimeoutMinutes;

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
     * 订单发货：仅「待发货」状态可操作（条件更新防止并发状态漂移）。
     */
    @Transactional
    public void deliver(Long id, DeliverDTO dto) {
        OmsOrder order = lockOrder(id);
        requireNoActiveRefund(id);
        if (order.getStatus() != STATUS_UNSHIPPED) {
            throw new BusinessException("当前订单状态不允许发货（仅待发货订单可发货）");
        }
        int affected = orderMapper.update(null, new LambdaUpdateWrapper<OmsOrder>()
                .set(OmsOrder::getStatus, STATUS_SHIPPED)
                .set(OmsOrder::getLogisticsCompany, dto.getLogisticsCompany())
                .set(OmsOrder::getLogisticsSn, dto.getLogisticsSn())
                .set(OmsOrder::getDeliveryTime, LocalDateTime.now())
                .eq(OmsOrder::getId, id)
                .eq(OmsOrder::getStatus, STATUS_UNSHIPPED));
        if (affected == 0) {
            throw new BusinessException("订单状态已变更，请刷新后重试");
        }
    }

    /**
     * 关闭未付款订单。付款以后的关闭由退款流程处理。
     */
    @Transactional
    public void close(Long id) {
        OmsOrder order = lockOrder(id);
        if (order.getStatus() != STATUS_UNPAID) {
            throw new BusinessException("仅待付款订单可以关闭，已付款订单请通过退款流程处理");
        }
        closeOrder(order);
    }

    /**
     * 查询超时未支付订单ID（供定时任务批量关单）。
     */
    public List<Long> listExpiredOrderIds(int timeoutMinutes) {
        return orderMapper.selectList(new LambdaQueryWrapper<OmsOrder>()
                        .select(OmsOrder::getId)
                        .eq(OmsOrder::getStatus, STATUS_UNPAID)
                        .isNull(OmsOrder::getPaymentCheckTime)
                        .le(OmsOrder::getCreateTime, LocalDateTime.now().minusMinutes(timeoutMinutes))
                        .last("LIMIT 100"))
                .stream().map(OmsOrder::getId).toList();
    }

    /**
     * 支付超时关单：回补库存/销量/优惠券并关闭订单（定时任务单笔调用，事务隔离）。
     *
     * @return 是否实际关闭
     */
    @Transactional
    public boolean closeExpiredOrder(Long orderId, int timeoutMinutes) {
        OmsOrder order = orderMapper.lockById(orderId);
        if (order == null || order.getStatus() != STATUS_UNPAID || order.getPaymentCheckTime() != null) {
            return false;
        }
        if (order.getCreateTime().isAfter(LocalDateTime.now().minusMinutes(timeoutMinutes))) {
            return false;
        }
        closeOrder(order);
        return true;
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
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OrderDetailVO createOrder(Long memberId, OrderCreateDTO dto) {
        UmsMember buyer = memberMapper.lockById(memberId);
        OmsOrder previous = findCheckoutOrder(memberId, dto.getCheckoutRequestId());
        if (previous != null) {
            return toDetailVO(previous);
        }
        Map<Long, Integer> quantities = mergeQuantities(dto.getItems());
        // 商品写入使用相同的加锁顺序，防止下单时删除规格。
        List<PmsSku> skus = skuMapper.selectBatchIds(quantities.keySet());
        skus.stream().map(PmsSku::getProductId).distinct().sorted().forEach(productMapper::lockById);
        // 解析生效中的秒杀价（服务端权威价格，覆盖 SKU 常规售价）
        Map<Long, SmsFlashSale> flashMap = flashSaleService.activeSkuMap(quantities.keySet());

        // 秒杀每人限购校验（时间窗口内有效购买数量 + 本次请求数量不得超过限购数）
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            SmsFlashSale flash = flashMap.get(entry.getKey());
            if (flash == null) {
                continue;
            }
            int limit = flash.getPerUserLimit() == null ? 1 : flash.getPerUserLimit();
            int bought = itemMapper.sumPurchasedInWindow(memberId, entry.getKey(),
                    flash.getStartTime(), flash.getEndTime());
            if (bought + entry.getValue() > limit) {
                throw new BusinessException("该秒杀商品每人限购 " + limit + " 件，您已购买 " + bought + " 件");
            }
        }

        List<OmsOrderItem> items = priceItems(quantities, flashMap);
        Map<Long, BigDecimal> currentPrices = items.stream()
                .collect(Collectors.toMap(OmsOrderItem::getSkuId, OmsOrderItem::getPrice));
        for (OrderCreateDTO.Item requested : dto.getItems()) {
            if (requested.getExpectedUnitPrice() != null
                    && requested.getExpectedUnitPrice().compareTo(currentPrices.get(requested.getSkuId())) != 0) {
                throw new BusinessException("商品单价已变化，请核对后再提交");
            }
        }
        Pricing pricing = calculatePrice(buyer, dto.getCouponMemberId(), items);
        OrderQuoteVO quote = pricing.quote();
        if (dto.getExpectedPayAmount() != null && dto.getExpectedPayAmount().compareTo(quote.payAmount()) != 0) {
            throw new BusinessException("结算金额已变化，请核对金额后再提交");
        }

        for (OmsOrderItem item : items) {
            int quantity = item.getQuantity();
            // 条件扣减：stock >= quantity 才执行，杜绝超卖
            int affected = skuMapper.update(null, new LambdaUpdateWrapper<PmsSku>()
                    .setSql("stock = stock - " + quantity)
                    .eq(PmsSku::getId, item.getSkuId())
                    .ge(PmsSku::getStock, quantity));
            if (affected == 0) {
                throw new BusinessException("「" + item.getProductName() + "」库存不足");
            }
            productMapper.update(null, new LambdaUpdateWrapper<PmsProduct>()
                    .setSql("stock = GREATEST(stock - " + quantity + ", 0)")
                    .eq(PmsProduct::getId, item.getProductId()));
        }

        OmsOrder order = new OmsOrder();
        order.setOrderSn(generateOrderSn());
        order.setMemberId(memberId);
        order.setCheckoutRequestId(dto.getCheckoutRequestId());
        order.setTotalAmount(quote.totalAmount());
        order.setCouponAmount(quote.couponAmount());
        order.setFreightAmount(quote.freightAmount());
        order.setPayAmount(quote.payAmount());
        order.setCouponId(pricing.coupon() == null ? null : pricing.coupon().getId());
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
        if (pricing.couponMember() != null) {
            int claimed = couponMemberMapper.update(null, new LambdaUpdateWrapper<SmsCouponMember>()
                    .set(SmsCouponMember::getStatus, 1)
                    .set(SmsCouponMember::getOrderId, order.getId())
                    .set(SmsCouponMember::getUseTime, LocalDateTime.now())
                    .eq(SmsCouponMember::getId, pricing.couponMember().getId())
                    .eq(SmsCouponMember::getStatus, 0));
            if (claimed == 0) {
                throw new BusinessException("优惠券已被使用，请重新选择");
            }
            LocalDateTime now = LocalDateTime.now();
            int recorded = couponMapper.update(null, new LambdaUpdateWrapper<SmsCoupon>()
                    .setSql("used_count = used_count + 1")
                    .eq(SmsCoupon::getId, pricing.coupon().getId())
                    .eq(SmsCoupon::getStatus, 1)
                    .le(SmsCoupon::getStartTime, now)
                    .ge(SmsCoupon::getEndTime, now));
            if (recorded == 0) {
                throw new BusinessException("优惠券已失效");
            }
        }
        if (order.getPayAmount().signum() == 0) {
            markPaid(order, LocalDateTime.now());
        }
        return toDetailVO(orderMapper.selectById(order.getId()));
    }

    public OrderDetailVO detailByCheckoutRequest(Long memberId, String requestId) {
        OmsOrder order = findCheckoutOrder(memberId, requestId);
        return order == null ? null : toDetailVO(order);
    }

    private OmsOrder findCheckoutOrder(Long memberId, String requestId) {
        return requestId == null ? null : orderMapper.selectOne(new LambdaQueryWrapper<OmsOrder>()
                .eq(OmsOrder::getMemberId, memberId)
                .eq(OmsOrder::getCheckoutRequestId, requestId));
    }

    @Transactional(readOnly = true)
    public OrderQuoteVO quote(Long memberId, OrderQuoteDTO dto) {
        Map<Long, Integer> quantities = mergeQuantities(dto.getItems());
        List<OmsOrderItem> items = priceItems(quantities, flashSaleService.activeSkuMap(quantities.keySet()));
        return calculatePrice(memberMapper.selectById(memberId), dto.getCouponMemberId(), items).quote();
    }

    private Map<Long, Integer> mergeQuantities(List<OrderCreateDTO.Item> requestedItems) {
        // 合并相同 SKU 的数量
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (OrderCreateDTO.Item item : requestedItems) {
            try {
                quantities.merge(item.getSkuId(), item.getQuantity(), Math::addExact);
            } catch (ArithmeticException e) {
                throw new BusinessException("购买数量超出范围");
            }
        }
        return quantities;
    }

    private List<OmsOrderItem> priceItems(Map<Long, Integer> quantities, Map<Long, SmsFlashSale> flashMap) {
        List<OmsOrderItem> items = new ArrayList<>();
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
            if (sku.getStock() == null || sku.getStock() < quantity) {
                throw new BusinessException("「" + product.getName() + "」库存不足");
            }
            BigDecimal unitPrice = flashMap.containsKey(sku.getId())
                    ? flashMap.get(sku.getId()).getFlashPrice()
                    : sku.getPrice();
            OmsOrderItem item = new OmsOrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setProductPic(product.getMainPic());
            item.setSkuId(sku.getId());
            item.setSkuSpecs(sku.getSpecs());
            item.setPrice(unitPrice);
            item.setQuantity(quantity);
            item.setItemAmount(unitPrice.multiply(BigDecimal.valueOf(quantity))
                    .setScale(2, RoundingMode.HALF_UP));
            items.add(item);
        }
        return items;
    }

    private Pricing calculatePrice(UmsMember buyer, Long couponMemberId, List<OmsOrderItem> items) {
        BigDecimal totalAmount = items.stream().map(OmsOrderItem::getItemAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // 运费规则：金卡及以上会员全场免运费；其余满 99 包邮
        int buyerLevel = buyer == null || buyer.getLevel() == null ? 1 : buyer.getLevel();
        BigDecimal freightAmount = MemberLevels.freeShipping(buyerLevel)
                ? BigDecimal.ZERO
                : (totalAmount.compareTo(FREE_FREIGHT_THRESHOLD) < 0 ? FREIGHT_AMOUNT : BigDecimal.ZERO);

        // 优惠券校验与抵扣计算
        BigDecimal couponAmount = BigDecimal.ZERO;
        SmsCouponMember usedCouponMember = null;
        SmsCoupon usedCoupon = null;
        if (couponMemberId != null) {
            usedCouponMember = couponMemberMapper.selectById(couponMemberId);
            if (usedCouponMember == null || buyer == null || !usedCouponMember.getMemberId().equals(buyer.getId())
                    || usedCouponMember.getStatus() != 0) {
                throw new BusinessException("优惠券不可用");
            }
            usedCoupon = couponMapper.selectById(usedCouponMember.getCouponId());
            LocalDateTime now = LocalDateTime.now();
            if (!CouponService.isUsableNow(usedCoupon, now)) {
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
        BigDecimal payAmount = totalAmount.subtract(couponAmount).add(freightAmount);
        return new Pricing(new OrderQuoteVO(items, totalAmount, freightAmount, couponAmount, payAmount),
                usedCouponMember, usedCoupon);
    }

    private record Pricing(OrderQuoteVO quote, SmsCouponMember couponMember, SmsCoupon coupon) {}

    /**
     * 发起支付：返回支付宝收银台跳转 URL。
     * 生产语义：未配置支付渠道时明确报错，**绝不**以模拟入账代替真实支付。
     */
    @Transactional
    public Map<String, Object> pay(Long memberId, Long id, String returnHost) {
        OmsOrder order = requireOrderOwner(lockOrder(id), memberId);
        if (order.getStatus() != STATUS_UNPAID) {
            throw new BusinessException("当前订单状态不可支付");
        }
        LocalDateTime paymentDeadline = paymentDeadline(order);
        if (!paymentDeadline.isAfter(LocalDateTime.now())) {
            throw new BusinessException("订单已超过付款时限");
        }
        if (order.getPayAmount().signum() == 0) {
            markPaid(order, LocalDateTime.now());
            return Map.of("paid", true);
        }
        AlipayService.PaymentPage page = alipayService.buildPagePayUrl(order, paymentDeadline, returnHost);
        if (order.getPaymentCheckTime() == null || order.getPaymentProfileId() == null) {
            orderMapper.update(null, new LambdaUpdateWrapper<OmsOrder>()
                    .set(OmsOrder::getPaymentProfileId, page.profileId())
                    .set(order.getPaymentCheckTime() == null, OmsOrder::getPaymentCheckTime, LocalDateTime.now())
                    .eq(OmsOrder::getId, order.getId()));
        }
        return Map.of("payUrl", page.url());
    }

    @Transactional(readOnly = true)
    public boolean verifyPaymentNotice(String orderSn, Map<String, String> params) {
        if (!StringUtils.hasText(orderSn)) {
            return false;
        }
        OmsOrder order = orderMapper.selectOne(new LambdaQueryWrapper<OmsOrder>()
                .eq(OmsOrder::getOrderSn, orderSn));
        return order != null && alipayService.verifyNotify(order, params);
    }

    public List<OmsOrder> listOrdersForPaymentCheck(LocalDateTime checkBefore) {
        return orderMapper.selectList(new LambdaQueryWrapper<OmsOrder>()
                .in(OmsOrder::getStatus, STATUS_UNPAID, STATUS_CLOSED)
                .le(OmsOrder::getPaymentCheckTime, checkBefore)
                .orderByAsc(OmsOrder::getPaymentCheckTime, OmsOrder::getId)
                .last("LIMIT 10"));
    }

    public boolean claimPaymentCheck(OmsOrder order, LocalDateTime checkBefore) {
        return orderMapper.update(null, new LambdaUpdateWrapper<OmsOrder>()
                .set(OmsOrder::getPaymentCheckTime, LocalDateTime.now())
                .eq(OmsOrder::getId, order.getId())
                .eq(OmsOrder::getStatus, order.getStatus())
                .le(OmsOrder::getPaymentCheckTime, checkBefore)) > 0;
    }

    public boolean isPaymentExpired(OmsOrder order) {
        return !paymentDeadline(order).isAfter(LocalDateTime.now());
    }

    @Transactional
    public void finishUnpaidPaymentCheck(Long orderId) {
        OmsOrder order = lockOrder(orderId);
        if (order.getPaymentCheckTime() == null) {
            return;
        }
        if (order.getStatus() == STATUS_UNPAID) {
            closeOrder(order);
        }
        setPaymentCheckTime(orderId, null);
    }

    private void setPaymentCheckTime(Long orderId, LocalDateTime time) {
        orderMapper.update(null, new LambdaUpdateWrapper<OmsOrder>()
                .set(OmsOrder::getPaymentCheckTime, time)
                .eq(OmsOrder::getId, orderId));
    }

    /**
     * 支付入账（支付宝通知或签名查单调用）：以订单号定位，金额校验 + 幂等。
     */
    @Transactional
    public void payByOrderSn(String orderSn, BigDecimal paidAmount, LocalDateTime paidAt) {
        OmsOrder order = orderMapper.selectOne(new LambdaQueryWrapper<OmsOrder>()
                .eq(OmsOrder::getOrderSn, orderSn).last("FOR UPDATE"));
        if (order == null) {
            throw new BusinessException("订单不存在：" + orderSn);
        }
        if (paidAmount == null || paidAmount.compareTo(order.getPayAmount()) != 0) {
            throw new BusinessException("支付金额与订单金额不符");
        }
        if (order.getStatus() == STATUS_CLOSED && order.getPayTime() == null) {
            queueLatePaymentRefund(order);
            if (order.getPaymentCheckTime() != null) {
                setPaymentCheckTime(order.getId(), null);
            }
        } else if (order.getStatus() == STATUS_UNPAID) {
            markPaid(order, paidAt);
        }
    }

    private void queueLatePaymentRefund(OmsOrder order) {
        Long existing = returnMapper.selectCount(new LambdaQueryWrapper<OmsOrderReturn>()
                .eq(OmsOrderReturn::getOrderId, order.getId())
                .in(OmsOrderReturn::getStatus, OmsOrderReturn.PROCESSING, OmsOrderReturn.REFUNDED));
        if (existing > 0) {
            return;
        }
        OmsOrderReturn refund = new OmsOrderReturn();
        refund.setOrderId(order.getId());
        refund.setOrderSn(order.getOrderSn());
        refund.setMemberId(order.getMemberId());
        refund.setReason("关闭订单收到付款，自动原路退款");
        refund.setAmount(order.getPayAmount());
        refund.setStatus(OmsOrderReturn.PROCESSING);
        returnMapper.insert(refund);
    }

    /**
     * 标记订单已支付：条件更新（防并发重复入账）→ 累计销量 → 会员成长值与积分批次入账。
     * 返回 false 表示订单已被其他请求处理（CAS 抢占失败）。
     */
    private boolean markPaid(OmsOrder order, LocalDateTime paidAt) {
        int affected = orderMapper.update(null, new LambdaUpdateWrapper<OmsOrder>()
                .set(OmsOrder::getStatus, STATUS_UNSHIPPED)
                .set(OmsOrder::getPayTime, paidAt)
                .set(OmsOrder::getPaymentCheckTime, null)
                .eq(OmsOrder::getId, order.getId())
                .eq(OmsOrder::getStatus, STATUS_UNPAID));
        if (affected == 0) {
            return false;
        }
        UmsMember member = memberMapper.lockById(order.getMemberId());
        List<OmsOrderItem> items = listItems(order.getId());
        lockOrderProducts(items);
        // 支付成功累计销量
        for (OmsOrderItem item : items) {
            skuMapper.update(null, new LambdaUpdateWrapper<PmsSku>()
                    .setSql("sales = sales + " + item.getQuantity())
                    .eq(PmsSku::getId, item.getSkuId()));
            productMapper.update(null, new LambdaUpdateWrapper<PmsProduct>()
                    .setSql("sales = sales + " + item.getQuantity())
                    .eq(PmsProduct::getId, item.getProductId()));
        }
        // 会员成长（双账户模型）：
        // 成长值 += 实付金额（只增不减，决定等级——积分消费不会掉级）；
        // 积分 += 实付金额 × 等级倍率（站内货币，可在积分商城兑换优惠券）；
        // 同时写入积分批次账本（12 个月滚动过期）。
        if (member != null && order.getPayAmount().signum() > 0) {
            int payYuan = order.getPayAmount().intValue();
            int currentGrowth = member.getGrowth() == null ? 0 : member.getGrowth();
            int currentPoints = member.getPoints() == null ? 0 : member.getPoints();
            int earnedPoints = order.getPayAmount()
                    .multiply(BigDecimal.valueOf(MemberLevels.pointPercent(member.getLevel()), 2)).intValue();
            int newGrowth = currentGrowth + payYuan;
            UmsMember memberUpdate = new UmsMember();
            memberUpdate.setId(member.getId());
            memberUpdate.setPoints(currentPoints + earnedPoints);
            memberUpdate.setGrowth(newGrowth);
            memberUpdate.setLevel(MemberLevels.resolveLevel(newGrowth));
            memberMapper.updateById(memberUpdate);
            pointsLedgerService.earnForOrder(order, earnedPoints);
        }
        return true;
    }

    /**
     * 会员取消订单（仅待付款），回补库存与优惠券（条件更新防并发）。
     */
    @Transactional
    public void cancel(Long memberId, Long id) {
        OmsOrder order = requireOrderOwner(lockOrder(id), memberId);
        if (order.getStatus() != STATUS_UNPAID) {
            throw new BusinessException("仅待付款订单可以取消，已支付订单请申请退款");
        }
        closeOrder(order);
    }

    /**
     * 确认收货：已发货 → 已完成（条件更新防并发）。
     */
    @Transactional
    public void receive(Long memberId, Long id) {
        OmsOrder order = requireOrderOwner(lockOrder(id), memberId);
        requireNoActiveRefund(id);
        if (order.getStatus() != STATUS_SHIPPED) {
            throw new BusinessException("当前订单状态不可确认收货");
        }
        int affected = orderMapper.update(null, new LambdaUpdateWrapper<OmsOrder>()
                .set(OmsOrder::getStatus, STATUS_COMPLETED)
                .set(OmsOrder::getReceiveTime, LocalDateTime.now())
                .eq(OmsOrder::getId, id)
                .eq(OmsOrder::getStatus, STATUS_SHIPPED));
        if (affected == 0) {
            throw new BusinessException("订单状态已变更，请刷新后重试");
        }
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
     * 先关闭订单，再回补库存、销量和优惠券。调用者持有订单行锁。
     */
    void closeOrder(OmsOrder order) {
        int affected = orderMapper.update(null, new LambdaUpdateWrapper<OmsOrder>()
                .set(OmsOrder::getStatus, STATUS_CLOSED)
                .set(OmsOrder::getCloseTime, LocalDateTime.now())
                .eq(OmsOrder::getId, order.getId())
                .eq(OmsOrder::getStatus, order.getStatus()));
        if (affected == 0) {
            throw new BusinessException("订单状态已变更，请刷新后重试");
        }
        restoreStockAndCoupon(order);
    }

    private void restoreStockAndCoupon(OmsOrder order) {
        List<OmsOrderItem> items = listItems(order.getId());
        lockOrderProducts(items);
        boolean paid = order.getStatus() == STATUS_UNSHIPPED || order.getStatus() == STATUS_SHIPPED
                || order.getStatus() == STATUS_COMPLETED;
        for (OmsOrderItem item : items) {
            int quantity = item.getQuantity();
            int restored = skuMapper.update(null, new LambdaUpdateWrapper<PmsSku>()
                    .setSql("stock = stock + " + quantity)
                    .eq(PmsSku::getId, item.getSkuId())
                    .eq(PmsSku::getProductId, item.getProductId()));
            if (restored > 0) {
                productMapper.update(null, new LambdaUpdateWrapper<PmsProduct>()
                        .setSql("stock = stock + " + quantity)
                        .eq(PmsProduct::getId, item.getProductId()));
            }
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

    OmsOrder lockOrder(Long id) {
        OmsOrder order = orderMapper.lockById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    void requireNoActiveRefund(Long orderId) {
        Long active = returnMapper.selectCount(new LambdaQueryWrapper<OmsOrderReturn>()
                .eq(OmsOrderReturn::getOrderId, orderId)
                .in(OmsOrderReturn::getStatus, OmsOrderReturn.PENDING, OmsOrderReturn.PROCESSING, OmsOrderReturn.AWAITING_STOCK));
        if (active > 0) {
            throw new BusinessException("订单有退款正在处理");
        }
    }

    private OmsOrder getMemberOrder(Long memberId, Long id) {
        return requireOrderOwner(orderMapper.selectById(id), memberId);
    }

    private OmsOrder requireOrderOwner(OmsOrder order, Long memberId) {
        if (order == null || !order.getMemberId().equals(memberId)) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    void lockProductsForOrder(Long orderId) {
        lockOrderProducts(listItems(orderId));
    }

    private void lockOrderProducts(List<OmsOrderItem> items) {
        items.stream().map(OmsOrderItem::getProductId).distinct().sorted().forEach(productMapper::lockById);
    }

    private LocalDateTime paymentDeadline(OmsOrder order) {
        return order.getCreateTime().plusMinutes(payTimeoutMinutes);
    }

    static LocalDateTime returnDeadline(OmsOrder order) {
        return order.getReceiveTime() == null ? null : order.getReceiveTime().plusDays(RETURN_DAYS);
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
        Map<Long, Set<Long>> reviewedProducts = commentMapper.selectList(new LambdaQueryWrapper<PmsComment>()
                        .select(PmsComment::getOrderId, PmsComment::getProductId)
                        .in(PmsComment::getOrderId, orderIds))
                .stream().collect(Collectors.groupingBy(PmsComment::getOrderId,
                        Collectors.mapping(PmsComment::getProductId, Collectors.toSet())));

        Map<Long, Integer> refundStatuses = returnMapper.selectList(new LambdaQueryWrapper<OmsOrderReturn>()
                        .select(OmsOrderReturn::getOrderId, OmsOrderReturn::getStatus)
                        .in(OmsOrderReturn::getOrderId, orderIds).orderByDesc(OmsOrderReturn::getId))
                .stream().collect(Collectors.toMap(OmsOrderReturn::getOrderId, OmsOrderReturn::getStatus,
                        (latest, older) -> latest));

        return orders.stream().map(order -> {
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(order, vo);
            UmsMember member = memberMap.get(order.getMemberId());
            vo.setMemberNickname(member == null ? "" : member.getNickname());
            vo.setRefundStatus(refundStatuses.get(order.getId()));
            if (order.getStatus() == STATUS_UNPAID) {
                vo.setPaymentDeadline(paymentDeadline(order));
            }
            if (order.getStatus() == STATUS_COMPLETED) {
                vo.setReturnDeadline(returnDeadline(order));
            }
            List<OmsOrderItem> items = itemsMap.getOrDefault(order.getId(), List.of());
            Set<Long> reviewed = reviewedProducts.getOrDefault(order.getId(), Set.of());
            vo.setCommented(!items.isEmpty() && items.stream().allMatch(item -> reviewed.contains(item.getProductId())));
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
        if (order.getStatus() == STATUS_COMPLETED) {
            vo.setReviewedProductIds(commentMapper.selectList(new LambdaQueryWrapper<PmsComment>()
                            .select(PmsComment::getProductId).eq(PmsComment::getOrderId, order.getId()))
                    .stream().map(PmsComment::getProductId).toList());
        }
        vo.setReturnApply(returnMapper.selectOne(new LambdaQueryWrapper<OmsOrderReturn>()
                .eq(OmsOrderReturn::getOrderId, order.getId())
                .orderByDesc(OmsOrderReturn::getId)
                .last("LIMIT 1")));
        return vo;
    }
}
