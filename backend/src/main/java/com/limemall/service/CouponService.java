package com.limemall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limemall.common.BusinessException;
import com.limemall.common.PageResult;
import com.limemall.dto.CouponSaveDTO;
import com.limemall.entity.SmsCoupon;
import com.limemall.entity.SmsCouponMember;
import com.limemall.entity.UmsMember;
import com.limemall.mapper.SmsCouponMapper;
import com.limemall.mapper.SmsCouponMemberMapper;
import com.limemall.mapper.UmsMemberMapper;
import com.limemall.vo.CouponRecordVO;
import com.limemall.vo.MemberCouponVO;
import com.limemall.vo.PortalCouponVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 优惠券服务（管理员端）：增删改查、上下线、领取记录查询。
 */
@Service
@RequiredArgsConstructor
public class CouponService {

    private final SmsCouponMapper couponMapper;
    private final SmsCouponMemberMapper couponMemberMapper;
    private final UmsMemberMapper memberMapper;
    private final PointsLedgerService pointsLedgerService;

    /** 分页查询优惠券（名称模糊、状态筛选） */
    public PageResult<SmsCoupon> page(long pageNum, long pageSize, String name, Integer status) {
        LambdaQueryWrapper<SmsCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(name), SmsCoupon::getName, name)
                .eq(status != null, SmsCoupon::getStatus, status)
                .orderByDesc(SmsCoupon::getId);
        Page<SmsCoupon> page = couponMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page);
    }

    /** 新增优惠券（默认下线状态，需手工上线） */
    public void create(CouponSaveDTO dto) {
        validateTimeRange(dto);
        SmsCoupon coupon = new SmsCoupon();
        applyDto(coupon, dto);
        coupon.setReceiveCount(0);
        coupon.setUsedCount(0);
        coupon.setStatus(0);
        couponMapper.insert(coupon);
    }

    /** 编辑优惠券（发行总量不得少于已领取数量） */
    public void update(CouponSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("缺少优惠券ID");
        }
        SmsCoupon existing = couponMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BusinessException("优惠券不存在");
        }
        validateTimeRange(dto);
        if (dto.getTotalCount() < existing.getReceiveCount()) {
            throw new BusinessException("发行总量不能小于已领取数量（" + existing.getReceiveCount() + "）");
        }
        SmsCoupon changes = new SmsCoupon();
        applyDto(changes, dto);
        int updated = couponMapper.update(changes, new LambdaUpdateWrapper<SmsCoupon>()
                .eq(SmsCoupon::getId, dto.getId())
                .le(SmsCoupon::getReceiveCount, dto.getTotalCount()));
        if (updated == 0) {
            throw new BusinessException("已领取数量已变化，请刷新后重新编辑");
        }
    }

    /** 上线/下线优惠券 */
    public void publish(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态参数不正确");
        }
        SmsCoupon coupon = couponMapper.selectById(id);
        if (coupon == null) {
            throw new BusinessException("优惠券不存在");
        }
        if (status == 1 && coupon.getEndTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("优惠券已过期，无法上线");
        }
        SmsCoupon update = new SmsCoupon();
        update.setId(id);
        update.setStatus(status);
        couponMapper.updateById(update);
    }

    /** 分页查询某优惠券的领取记录 */
    public PageResult<CouponRecordVO> records(Long couponId, long pageNum, long pageSize) {
        Page<SmsCouponMember> page = couponMemberMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SmsCouponMember>()
                        .eq(SmsCouponMember::getCouponId, couponId)
                        .orderByDesc(SmsCouponMember::getId));
        List<SmsCouponMember> records = page.getRecords();
        if (records.isEmpty()) {
            return PageResult.of(page, new ArrayList<>());
        }
        Map<Long, UmsMember> memberMap = memberMapper.selectBatchIds(
                        records.stream().map(SmsCouponMember::getMemberId).distinct().toList())
                .stream().collect(Collectors.toMap(UmsMember::getId, Function.identity()));
        List<CouponRecordVO> vos = records.stream().map(record -> {
            CouponRecordVO vo = new CouponRecordVO();
            vo.setId(record.getId());
            UmsMember member = memberMap.get(record.getMemberId());
            if (member != null) {
                vo.setMemberUsername(member.getUsername());
                vo.setMemberNickname(member.getNickname());
            }
            vo.setStatus(record.getStatus());
            vo.setReceiveTime(record.getReceiveTime());
            vo.setUseTime(record.getUseTime());
            vo.setOrderId(record.getOrderId());
            return vo;
        }).toList();
        return PageResult.of(page, vos);
    }

    // ==================================================================
    // 商城前台（会员端）
    // ==================================================================

    /**
     * 领券中心：当前可领取的优惠券列表（含是否已达限领）。
     */
    public List<PortalCouponVO> available(Long memberId) {
        LocalDateTime now = LocalDateTime.now();
        List<SmsCoupon> coupons = couponMapper.selectList(new LambdaQueryWrapper<SmsCoupon>()
                .eq(SmsCoupon::getStatus, 1)
                .le(SmsCoupon::getStartTime, now)
                .ge(SmsCoupon::getEndTime, now)
                .apply("receive_count < total_count")
                .orderByDesc(SmsCoupon::getId));
        Map<Long, Long> myReceived = couponMemberMapper.selectList(
                        new LambdaQueryWrapper<SmsCouponMember>()
                                .eq(SmsCouponMember::getMemberId, memberId))
                .stream().collect(Collectors.groupingBy(SmsCouponMember::getCouponId, Collectors.counting()));
        return coupons.stream().map(coupon -> {
            PortalCouponVO vo = new PortalCouponVO();
            vo.setId(coupon.getId());
            vo.setName(coupon.getName());
            vo.setType(coupon.getType());
            vo.setAmount(coupon.getAmount());
            vo.setMinAmount(coupon.getMinAmount());
            vo.setStartTime(coupon.getStartTime());
            vo.setEndTime(coupon.getEndTime());
            vo.setRemaining(coupon.getTotalCount() - coupon.getReceiveCount());
            vo.setReceived(myReceived.getOrDefault(coupon.getId(), 0L) >= coupon.getPerLimit());
            return vo;
        }).toList();
    }

    /**
     * 领取优惠券。
     *
     * 防超发：对 receive_count 做条件更新（receive_count < total_count），
     * 影响行数为 0 说明已被领完，事务回滚。
     */
    @Transactional
    public void receive(Long memberId, Long couponId) {
        memberMapper.lockById(memberId);
        SmsCoupon coupon = couponMapper.selectById(couponId);
        LocalDateTime now = LocalDateTime.now();
        if (coupon == null || coupon.getStatus() != 1) {
            throw new BusinessException("优惠券不存在或已停止发放");
        }
        if (now.isBefore(coupon.getStartTime()) || now.isAfter(coupon.getEndTime())) {
            throw new BusinessException("不在优惠券领取时间内");
        }
        Long myCount = couponMemberMapper.selectCount(new LambdaQueryWrapper<SmsCouponMember>()
                .eq(SmsCouponMember::getCouponId, couponId)
                .eq(SmsCouponMember::getMemberId, memberId));
        if (myCount >= coupon.getPerLimit()) {
            throw new BusinessException("已达到每人限领数量");
        }
        int affected = couponMapper.update(null, new LambdaUpdateWrapper<SmsCoupon>()
                .setSql("receive_count = receive_count + 1")
                .eq(SmsCoupon::getId, couponId)
                .eq(SmsCoupon::getStatus, 1)
                .le(SmsCoupon::getStartTime, now)
                .ge(SmsCoupon::getEndTime, now)
                .apply("receive_count < total_count"));
        if (affected == 0) {
            throw new BusinessException("优惠券已被领完");
        }
        SmsCouponMember couponMember = new SmsCouponMember();
        couponMember.setCouponId(couponId);
        couponMember.setMemberId(memberId);
        couponMember.setStatus(0);
        couponMember.setReceiveTime(now);
        couponMemberMapper.insert(couponMember);
    }

    /**
     * 我的优惠券（含过期状态即时计算）。
     */
    public List<MemberCouponVO> mine(Long memberId) {
        List<SmsCouponMember> records = couponMemberMapper.selectList(
                new LambdaQueryWrapper<SmsCouponMember>()
                        .eq(SmsCouponMember::getMemberId, memberId)
                        .orderByDesc(SmsCouponMember::getId));
        if (records.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, SmsCoupon> couponMap = couponMapper.selectBatchIds(
                        records.stream().map(SmsCouponMember::getCouponId).distinct().toList())
                .stream().collect(Collectors.toMap(SmsCoupon::getId, Function.identity()));
        LocalDateTime now = LocalDateTime.now();
        return records.stream().map(record -> {
            SmsCoupon coupon = couponMap.get(record.getCouponId());
            if (coupon == null) {
                return null;
            }
            MemberCouponVO vo = new MemberCouponVO();
            vo.setId(record.getId());
            vo.setCouponId(coupon.getId());
            vo.setName(coupon.getName());
            vo.setType(coupon.getType());
            vo.setAmount(coupon.getAmount());
            vo.setMinAmount(coupon.getMinAmount());
            vo.setEndTime(coupon.getEndTime());
            // 未使用但已过期 → 展示为已过期
            int status = record.getStatus();
            if (status == 0 && !isUsableNow(coupon, now)) {
                status = coupon.getEndTime().isBefore(now) ? 2 : 3;
            }
            vo.setStatus(status);
            return vo;
        }).filter(vo -> vo != null).toList();
    }

    /**
     * 积分商城：可兑换优惠券列表（仅限 status=1 且在有效期内、设置了积分价的券）。
     */
    public List<PortalCouponVO> exchangeList() {
        LocalDateTime now = LocalDateTime.now();
        List<SmsCoupon> coupons = couponMapper.selectList(new LambdaQueryWrapper<SmsCoupon>()
                .eq(SmsCoupon::getStatus, 1)
                .gt(SmsCoupon::getPointsPrice, 0)
                .le(SmsCoupon::getStartTime, now)
                .ge(SmsCoupon::getEndTime, now)
                .orderByAsc(SmsCoupon::getPointsPrice));
        return coupons.stream().map(coupon -> {
            PortalCouponVO vo = new PortalCouponVO();
            vo.setId(coupon.getId());
            vo.setName(coupon.getName());
            vo.setType(coupon.getType());
            vo.setAmount(coupon.getAmount());
            vo.setMinAmount(coupon.getMinAmount());
            vo.setStartTime(coupon.getStartTime());
            vo.setEndTime(coupon.getEndTime());
            vo.setRemaining(coupon.getTotalCount() - coupon.getReceiveCount());
            vo.setPointsPrice(coupon.getPointsPrice());
            vo.setReceived(false);
            return vo;
        }).toList();
    }

    /**
     * 积分兑换优惠券。
     *
     * 并发安全：
     * - 积分扣减使用条件更新（points >= 兑换价），余额不足时影响行数为 0；
     * - 券发放使用条件更新（receive_count < total_count）防超兑；
     * - 全程同一事务，任一步失败自动回滚。
     */
    @Transactional
    public void exchange(Long memberId, Long couponId) {
        UmsMember member = memberMapper.lockById(memberId);
        SmsCoupon coupon = couponMapper.selectById(couponId);
        LocalDateTime now = LocalDateTime.now();
        if (coupon == null || coupon.getStatus() != 1
                || coupon.getPointsPrice() == null || coupon.getPointsPrice() <= 0) {
            throw new BusinessException("该优惠券不支持积分兑换");
        }
        if (now.isBefore(coupon.getStartTime()) || now.isAfter(coupon.getEndTime())) {
            throw new BusinessException("不在积分兑换有效期内");
        }
        Long myCount = couponMemberMapper.selectCount(new LambdaQueryWrapper<SmsCouponMember>()
                .eq(SmsCouponMember::getCouponId, couponId)
                .eq(SmsCouponMember::getMemberId, memberId));
        if (myCount >= coupon.getPerLimit()) {
            throw new BusinessException("已达到该券兑换上限");
        }
        pointsLedgerService.deduct(member, coupon.getPointsPrice(), "积分兑换：" + coupon.getName(), now);
        int stock = couponMapper.update(null, new LambdaUpdateWrapper<SmsCoupon>()
                .setSql("receive_count = receive_count + 1")
                .eq(SmsCoupon::getId, couponId)
                .eq(SmsCoupon::getStatus, 1)
                .le(SmsCoupon::getStartTime, now)
                .ge(SmsCoupon::getEndTime, now)
                .apply("receive_count < total_count"));
        if (stock == 0) {
            throw new BusinessException("优惠券已被兑完");
        }
        SmsCouponMember couponMember = new SmsCouponMember();
        couponMember.setCouponId(couponId);
        couponMember.setMemberId(memberId);
        couponMember.setStatus(0);
        couponMember.setReceiveTime(now);
        couponMemberMapper.insert(couponMember);
    }

    // ==================================================================
    // 私有方法
    // ==================================================================

    static boolean isUsableNow(SmsCoupon coupon, LocalDateTime now) {
        return coupon != null && coupon.getStatus() == 1
                && !now.isBefore(coupon.getStartTime()) && !now.isAfter(coupon.getEndTime());
    }

    private void validateTimeRange(CouponSaveDTO dto) {
        if (dto.getEndTime().isBefore(dto.getStartTime())) {
            throw new BusinessException("结束时间不能早于开始时间");
        }
        if (dto.getType() == 2 && dto.getAmount().compareTo(java.math.BigDecimal.TEN) >= 0) {
            throw new BusinessException("折扣券折扣值应小于 10（如 9.5 表示 95 折）");
        }
    }

    private void applyDto(SmsCoupon coupon, CouponSaveDTO dto) {
        coupon.setName(dto.getName());
        coupon.setType(dto.getType());
        coupon.setAmount(dto.getAmount());
        coupon.setMinAmount(dto.getMinAmount());
        coupon.setTotalCount(dto.getTotalCount());
        coupon.setPerLimit(dto.getPerLimit());
        coupon.setStartTime(dto.getStartTime());
        coupon.setEndTime(dto.getEndTime());
        coupon.setPointsPrice(dto.getPointsPrice() == null ? 0 : dto.getPointsPrice());
    }
}
