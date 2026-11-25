package com.limemall.init;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.limemall.entity.OmsOrder;
import com.limemall.entity.OmsOrderItem;
import com.limemall.entity.PmsComment;
import com.limemall.entity.PmsProduct;
import com.limemall.entity.PmsSku;
import com.limemall.entity.SmsCoupon;
import com.limemall.entity.SmsCouponMember;
import com.limemall.entity.UmsAdmin;
import com.limemall.entity.UmsMember;
import com.limemall.mapper.OmsOrderItemMapper;
import com.limemall.mapper.OmsOrderMapper;
import com.limemall.mapper.PmsCommentMapper;
import com.limemall.mapper.PmsProductMapper;
import com.limemall.mapper.PmsSkuMapper;
import com.limemall.mapper.SmsCouponMapper;
import com.limemall.mapper.SmsCouponMemberMapper;
import com.limemall.mapper.UmsAdminMapper;
import com.limemall.mapper.UmsMemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 演示数据初始化器（应用启动时执行）。
 *
 * 职责：
 * 1. 将 init.sql 中密码占位符 'INIT' 替换为 BCrypt 加密密码：
 *    管理员默认 admin123、会员默认 123456（避免在 SQL 中出现明文/固定哈希）；
 * 2. 首次启动时生成 60 条演示订单（日期相对当前时间滚动 30 天），
 *    使数据看板的趋势图在任何时间演示都有数据。
 *    生成过程同步维护 SKU 库存、销量与优惠券使用记录，保证数据自洽。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataInitializer implements CommandLineRunner {

    private final UmsAdminMapper adminMapper;
    private final UmsMemberMapper memberMapper;
    private final OmsOrderMapper orderMapper;
    private final OmsOrderItemMapper itemMapper;
    private final PmsCommentMapper commentMapper;
    private final PmsProductMapper productMapper;
    private final PmsSkuMapper skuMapper;
    private final SmsCouponMapper couponMapper;
    private final SmsCouponMemberMapper couponMemberMapper;
    private final PasswordEncoder passwordEncoder;

    /** 演示收货人 */
    private static final String[] RECEIVER_NAMES =
            {"林晚", "陈屿", "苏念", "周野", "沈青", "顾北", "许南", "江迟", "温言", "白露"};
    /** 演示收货地址 */
    private static final String[] RECEIVER_ADDRESSES = {
            "浙江省杭州市西湖区文三路 138 号",
            "上海市浦东新区世纪大道 100 号",
            "广东省深圳市南山区科技园南路 66 号",
            "江苏省南京市鼓楼区中山北路 200 号",
            "北京市朝阳区望京街 9 号",
            "四川省成都市武侯区天府大道 999 号",
            "湖北省武汉市洪山区珞瑜路 129 号",
            "福建省厦门市思明区望海路 21 号"
    };
    /** 订单状态循环模板：按 12 条一循环分布（待付款2、待发货4、已发货2、已完成3、已关闭1） */
    private static final int[] STATUS_PATTERN = {0, 0, 1, 1, 1, 2, 2, 3, 3, 3, 4, 1};

    @Override
    @Transactional
    public void run(String... args) {
        initPasswords();
        initDemoOrders();
        initDemoComments();
    }

    /**
     * 用 BCrypt 加密占位密码。
     */
    private void initPasswords() {
        List<UmsAdmin> admins = adminMapper.selectList(
                new LambdaQueryWrapper<UmsAdmin>().eq(UmsAdmin::getPassword, "INIT"));
        for (UmsAdmin admin : admins) {
            UmsAdmin update = new UmsAdmin();
            update.setId(admin.getId());
            update.setPassword(passwordEncoder.encode("admin123"));
            adminMapper.updateById(update);
            log.info("初始化管理员密码：{} / admin123", admin.getUsername());
        }
        List<UmsMember> members = memberMapper.selectList(
                new LambdaQueryWrapper<UmsMember>().eq(UmsMember::getPassword, "INIT"));
        for (UmsMember member : members) {
            UmsMember update = new UmsMember();
            update.setId(member.getId());
            update.setPassword(passwordEncoder.encode("123456"));
            memberMapper.updateById(update);
        }
        if (!members.isEmpty()) {
            log.info("初始化 {} 个会员的默认密码：123456", members.size());
        }
    }

    /**
     * 生成演示订单（幂等：已存在订单时跳过）。
     */
    private void initDemoOrders() {
        Long orderCount = orderMapper.selectCount(null);
        if (orderCount != null && orderCount > 0) {
            log.info("订单数据已存在，跳过演示订单生成");
            return;
        }
        log.info("开始生成演示订单数据...");

        List<PmsProduct> products = productMapper.selectList(
                new LambdaQueryWrapper<PmsProduct>().eq(PmsProduct::getPublishStatus, 1));
        Map<Long, List<PmsSku>> skuMap = skuMapper.selectList(null).stream()
                .collect(Collectors.groupingBy(PmsSku::getProductId));
        List<UmsMember> members = memberMapper.selectList(null);
        List<SmsCoupon> coupons = couponMapper.selectList(
                new LambdaQueryWrapper<SmsCoupon>().eq(SmsCoupon::getStatus, 1));
        if (products.isEmpty() || members.isEmpty()) {
            log.warn("商品或会员数据缺失，跳过演示订单生成");
            return;
        }

        Random random = new Random(42);
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        for (int i = 0; i < 60; i++) {
            int status = STATUS_PATTERN[i % STATUS_PATTERN.length];
            // 前 8 条落在最近数小时内（保证「今日」统计数据不为零），其余散布在近 30 天
            LocalDateTime createTime = i < 8
                    ? now.minusMinutes(10L + i * 25L).minusSeconds(random.nextInt(60))
                    : now.minusDays(1 + random.nextInt(29))
                            .minusHours(random.nextInt(20)).minusMinutes(random.nextInt(60));
            UmsMember member = members.get(random.nextInt(members.size()));

            // 随机挑选 1~3 个不同商品，每个商品选一个 SKU
            int itemCount = 1 + random.nextInt(3);
            Set<Integer> picked = new HashSet<>();
            while (picked.size() < itemCount) {
                picked.add(random.nextInt(products.size()));
            }
            List<OmsOrderItem> items = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (Integer productIndex : picked) {
                PmsProduct product = products.get(productIndex);
                List<PmsSku> skus = skuMap.getOrDefault(product.getId(), List.of());
                if (skus.isEmpty()) {
                    continue;
                }
                PmsSku sku = skus.get(random.nextInt(skus.size()));
                int quantity = 1 + random.nextInt(3);
                BigDecimal itemAmount = sku.getPrice()
                        .multiply(BigDecimal.valueOf(quantity))
                        .setScale(2, RoundingMode.HALF_UP);

                OmsOrderItem item = new OmsOrderItem();
                item.setProductId(product.getId());
                item.setProductName(product.getName());
                item.setProductPic(product.getMainPic());
                item.setSkuId(sku.getId());
                item.setSkuSpecs(sku.getSpecs());
                item.setPrice(sku.getPrice());
                item.setQuantity(quantity);
                item.setItemAmount(itemAmount);
                items.add(item);
                totalAmount = totalAmount.add(itemAmount);
            }
            if (items.isEmpty()) {
                continue;
            }
            totalAmount = totalAmount.setScale(2, RoundingMode.HALF_UP);

            // 运费规则：满 99 包邮
            BigDecimal freightAmount = totalAmount.compareTo(new BigDecimal("99")) < 0
                    ? new BigDecimal("10.00") : BigDecimal.ZERO;

            // 部分已支付订单使用优惠券（每 5 条取一），并维护领取使用记录
            BigDecimal couponAmount = BigDecimal.ZERO;
            SmsCoupon usedCoupon = null;
            if (status >= 1 && status <= 3 && i % 5 == 0 && !coupons.isEmpty()) {
                SmsCoupon coupon = coupons.get(i % coupons.size());
                if (totalAmount.compareTo(coupon.getMinAmount()) >= 0) {
                    if (coupon.getType() == 1) {
                        couponAmount = coupon.getAmount();
                    } else {
                        couponAmount = totalAmount
                                .multiply(BigDecimal.TEN.subtract(coupon.getAmount()))
                                .divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP);
                    }
                    if (couponAmount.compareTo(totalAmount) > 0) {
                        couponAmount = totalAmount;
                    }
                    usedCoupon = coupon;
                }
            }
            BigDecimal payAmount = totalAmount.subtract(couponAmount).add(freightAmount);

            OmsOrder order = new OmsOrder();
            order.setOrderSn("LM" + createTime.format(dateFormatter) + String.format("%06d", i + 1));
            order.setMemberId(member.getId());
            order.setTotalAmount(totalAmount);
            order.setCouponAmount(couponAmount);
            order.setFreightAmount(freightAmount);
            order.setPayAmount(payAmount);
            order.setCouponId(usedCoupon == null ? null : usedCoupon.getId());
            order.setStatus(status);
            order.setReceiverName(RECEIVER_NAMES[random.nextInt(RECEIVER_NAMES.length)]);
            order.setReceiverPhone("138" + (10000000 + random.nextInt(89999999)));
            order.setReceiverAddress(RECEIVER_ADDRESSES[random.nextInt(RECEIVER_ADDRESSES.length)]);
            if (random.nextInt(4) == 0) {
                order.setNote("请尽快发货，谢谢");
            }
            order.setCreateTime(createTime);
            if (status >= 1) {
                order.setPayTime(createTime.plusMinutes(2 + random.nextInt(20)));
            }
            if (status >= 2) {
                order.setDeliveryTime(order.getPayTime().plusHours(6 + random.nextInt(20)));
                order.setLogisticsCompany(random.nextBoolean() ? "顺丰速运" : "中通快递");
                order.setLogisticsSn("SF" + (100000000L + random.nextInt(899999999)));
            }
            if (status == 3) {
                order.setReceiveTime(order.getDeliveryTime().plusDays(1 + random.nextInt(3)));
            }
            if (status == 4) {
                order.setCloseTime(createTime.plusHours(2));
            }
            orderMapper.insert(order);

            for (OmsOrderItem item : items) {
                item.setOrderId(order.getId());
                item.setCreateTime(createTime);
                itemMapper.insert(item);

                // 未关闭订单占用库存
                if (status != 4) {
                    skuMapper.update(null, new LambdaUpdateWrapper<PmsSku>()
                            .setSql("stock = GREATEST(stock - " + item.getQuantity() + ", 0)")
                            .eq(PmsSku::getId, item.getSkuId()));
                    productMapper.update(null, new LambdaUpdateWrapper<PmsProduct>()
                            .setSql("stock = GREATEST(stock - " + item.getQuantity() + ", 0)")
                            .eq(PmsProduct::getId, item.getProductId()));
                }
                // 已支付订单累计销量
                if (status >= 1 && status <= 3) {
                    skuMapper.update(null, new LambdaUpdateWrapper<PmsSku>()
                            .setSql("sales = sales + " + item.getQuantity())
                            .eq(PmsSku::getId, item.getSkuId()));
                    productMapper.update(null, new LambdaUpdateWrapper<PmsProduct>()
                            .setSql("sales = sales + " + item.getQuantity())
                            .eq(PmsProduct::getId, item.getProductId()));
                }
            }

            // 优惠券使用记录 + 计数维护
            if (usedCoupon != null) {
                SmsCouponMember couponMember = new SmsCouponMember();
                couponMember.setCouponId(usedCoupon.getId());
                couponMember.setMemberId(member.getId());
                couponMember.setStatus(1);
                couponMember.setOrderId(order.getId());
                couponMember.setReceiveTime(createTime.minusDays(1));
                couponMember.setUseTime(order.getPayTime());
                couponMemberMapper.insert(couponMember);
                couponMapper.update(null, new LambdaUpdateWrapper<SmsCoupon>()
                        .setSql("used_count = used_count + 1, receive_count = receive_count + 1")
                        .eq(SmsCoupon::getId, usedCoupon.getId()));
            }
        }
        log.info("演示订单数据生成完成：60 条订单");
    }

    /**
     * 为已完成订单生成演示评价（幂等：已存在评价时跳过）。
     */
    private void initDemoComments() {
        Long count = commentMapper.selectCount(null);
        if (count != null && count > 0) {
            return;
        }
        List<OmsOrder> completed = orderMapper.selectList(new LambdaQueryWrapper<OmsOrder>()
                .eq(OmsOrder::getStatus, 3)
                .orderByDesc(OmsOrder::getId)
                .last("LIMIT 15"));
        if (completed.isEmpty()) {
            return;
        }
        String[] contents = {
                "东西不错，包装完好，物流也快，好评！",
                "第二次回购了，品质稳定，值得推荐。",
                "实物质感比图片还好，家里人都说不错。",
                "性价比很高，用了一周没有任何问题。",
                "发货速度很快，客服态度也很好，满意。",
                "整体满意，小瑕疵是外包装有一点点压痕。",
                "和描述一致，做工细致，五星好评。",
                "用起来很顺手，朋友看到也下单了。"
        };
        int[] ratings = {5, 5, 5, 4, 5, 4, 5, 5};
        int seeded = 0;
        for (OmsOrder order : completed) {
            List<OmsOrderItem> items = itemMapper.selectList(
                    new LambdaQueryWrapper<OmsOrderItem>()
                            .eq(OmsOrderItem::getOrderId, order.getId()));
            if (items.isEmpty()) {
                continue;
            }
            OmsOrderItem item = items.get(0);
            UmsMember member = memberMapper.selectById(order.getMemberId());
            PmsComment comment = new PmsComment();
            comment.setProductId(item.getProductId());
            comment.setOrderId(order.getId());
            comment.setMemberId(order.getMemberId());
            comment.setMemberNickname(member == null ? "" : member.getNickname());
            comment.setMemberLevel(member == null || member.getLevel() == null ? 1 : member.getLevel());
            comment.setRating(ratings[seeded % ratings.length]);
            comment.setContent(contents[seeded % contents.length]);
            commentMapper.insert(comment);
            seeded++;
        }
        if (seeded > 0) {
            log.info("演示评价数据生成完成：{} 条", seeded);
        }
    }
}
