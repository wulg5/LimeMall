package com.limemall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limemall.common.BusinessException;
import com.limemall.common.PageResult;
import com.limemall.entity.OmsOrder;
import com.limemall.entity.UmsMember;
import com.limemall.entity.UmsPointsLog;
import com.limemall.mapper.UmsMemberMapper;
import com.limemall.mapper.UmsPointsLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 积分批次账本服务。
 *
 * 规则：每笔积分获取形成独立批次，有效期 12 个月；消费按批次先进先出（FIFO）扣减；
 * 过期任务仅核销「仍有剩余且已到期」的批次，与会员积分余额通过事务保持一致。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointsLedgerService {

    private final UmsPointsLogMapper pointsLogMapper;
    private final UmsMemberMapper memberMapper;

    /** 记入获取批次（12 个月后过期） */
    public void earnForOrder(OmsOrder order, int points) {
        if (points <= 0) {
            return;
        }
        UmsPointsLog batch = new UmsPointsLog();
        batch.setMemberId(order.getMemberId());
        batch.setAmount(points);
        batch.setEarnedAmount(points);
        batch.setType(1);
        batch.setRemark(orderAwardRemark(order));
        batch.setStatus(0);
        batch.setExpireTime(LocalDateTime.now().plusMonths(12));
        pointsLogMapper.insert(batch);
    }

    /**
     * 消费积分：按批次先进先出扣减剩余额度。
     * 调用方已锁定会员行，余额和批次在同一事务内扣减。
     */
    public void deduct(UmsMember member, int points, String remark, LocalDateTime now) {
        if (member == null || member.getPoints() < points) {
            throw new BusinessException("可用积分不足");
        }
        Long memberId = member.getId();
        List<UmsPointsLog> batches = pointsLogMapper.selectList(new LambdaQueryWrapper<UmsPointsLog>()
                .eq(UmsPointsLog::getMemberId, memberId)
                .eq(UmsPointsLog::getType, 1)
                .eq(UmsPointsLog::getStatus, 0)
                .gt(UmsPointsLog::getAmount, 0)
                .gt(UmsPointsLog::getExpireTime, now)
                .orderByAsc(UmsPointsLog::getCreateTime)
                .orderByAsc(UmsPointsLog::getId));
        int remaining = points;
        for (UmsPointsLog batch : batches) {
            if (remaining <= 0) {
                break;
            }
            int take = Math.min(batch.getAmount(), remaining);
            batch.setAmount(batch.getAmount() - take);
            pointsLogMapper.updateById(batch);
            remaining -= take;
        }
        if (remaining > 0) {
            throw new BusinessException("可用积分不足");
        }
        subtractBalance(memberId, points);
        recordDebit(memberId, points, 2, 0, remark);
    }

    public List<Long> expiredBatchIdsAfter(long afterId, LocalDateTime cutoff) {
        return pointsLogMapper.selectList(new LambdaQueryWrapper<UmsPointsLog>()
                        .select(UmsPointsLog::getId)
                        .gt(UmsPointsLog::getId, afterId)
                        .eq(UmsPointsLog::getType, 1)
                        .eq(UmsPointsLog::getStatus, 0)
                        .gt(UmsPointsLog::getAmount, 0)
                        .le(UmsPointsLog::getExpireTime, cutoff)
                        .orderByAsc(UmsPointsLog::getId)
                        .last("LIMIT 500"))
                .stream().map(UmsPointsLog::getId).toList();
    }

    @Transactional
    public boolean expireBatch(Long batchId, LocalDateTime cutoff) {
        UmsPointsLog candidate = pointsLogMapper.selectById(batchId);
        if (candidate == null || memberMapper.lockById(candidate.getMemberId()) == null) {
            return false;
        }
        UmsPointsLog batch = pointsLogMapper.selectOne(new LambdaQueryWrapper<UmsPointsLog>()
                .eq(UmsPointsLog::getId, batchId)
                .eq(UmsPointsLog::getType, 1)
                .eq(UmsPointsLog::getStatus, 0)
                .gt(UmsPointsLog::getAmount, 0)
                .le(UmsPointsLog::getExpireTime, cutoff)
                .last("FOR UPDATE"));
        if (batch == null) {
            return false;
        }
        int amount = batch.getAmount();
        if (subtractBalance(batch.getMemberId(), amount) == 0) {
            log.warn("积分过期核销跳过（余额不足，数据异常）：batchId={} memberId={} amount={}",
                    batchId, batch.getMemberId(), amount);
            return false;
        }
        clearBatch(batchId, 1);
        recordDebit(batch.getMemberId(), amount, 3, 1, "积分过期（批次 #" + batchId + "）");
        return true;
    }

    /** 仅撤回该单尚未使用且未到期的积分，旧订单沿用原有的订单号备注。 */
    public void revokeForRefund(OmsOrder order) {
        UmsMember member = memberMapper.lockById(order.getMemberId());
        if (member == null) {
            return;
        }
        UmsPointsLog batch = pointsLogMapper.selectOne(new LambdaQueryWrapper<UmsPointsLog>()
                .eq(UmsPointsLog::getMemberId, member.getId())
                .eq(UmsPointsLog::getType, 1)
                .eq(UmsPointsLog::getStatus, 0)
                .eq(UmsPointsLog::getRemark, orderAwardRemark(order))
                .gt(UmsPointsLog::getAmount, 0)
                .gt(UmsPointsLog::getExpireTime, LocalDateTime.now())
                .last("FOR UPDATE"));
        if (batch == null) {
            return;
        }
        int amount = Math.min(member.getPoints(), batch.getAmount());
        if (amount < batch.getAmount()) {
            log.warn("退款撤回积分时发现批次超过余额：orderSn={} batchId={}", order.getOrderSn(), batch.getId());
        }
        clearBatch(batch.getId(), 2);
        if (amount > 0) {
            subtractBalance(member.getId(), amount);
            recordDebit(member.getId(), amount, 4, 2, "订单 " + order.getOrderSn() + " 退款撤回积分");
        }
    }

    private static String orderAwardRemark(OmsOrder order) {
        return "订单 " + order.getOrderSn() + " 消费积分";
    }

    private int subtractBalance(Long memberId, int points) {
        return memberMapper.update(null, new LambdaUpdateWrapper<UmsMember>()
                .setSql("points = points - " + points)
                .eq(UmsMember::getId, memberId)
                .ge(UmsMember::getPoints, points));
    }

    private void clearBatch(Long batchId, int status) {
        pointsLogMapper.update(null, new LambdaUpdateWrapper<UmsPointsLog>()
                .set(UmsPointsLog::getAmount, 0)
                .set(UmsPointsLog::getStatus, status)
                .eq(UmsPointsLog::getId, batchId));
    }

    private void recordDebit(Long memberId, int points, int type, int status, String remark) {
        UmsPointsLog record = new UmsPointsLog();
        record.setMemberId(memberId);
        record.setAmount(-points);
        record.setType(type);
        record.setStatus(status);
        record.setRemark(remark);
        pointsLogMapper.insert(record);
    }

    /** 会员积分明细（流水与批次，按时间倒序分页） */
    public PageResult<UmsPointsLog> records(Long memberId, long pageNum, long pageSize) {
        Page<UmsPointsLog> page = pointsLogMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<UmsPointsLog>()
                        .eq(UmsPointsLog::getMemberId, memberId)
                        .orderByDesc(UmsPointsLog::getId));
        return PageResult.of(page, page.getRecords());
    }
}
