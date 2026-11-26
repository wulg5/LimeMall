package com.limemall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limemall.common.PageResult;
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
    public void earn(Long memberId, int points, String remark) {
        if (points <= 0) {
            return;
        }
        UmsPointsLog batch = new UmsPointsLog();
        batch.setMemberId(memberId);
        batch.setAmount(points);
        batch.setType(1);
        batch.setRemark(remark);
        batch.setStatus(0);
        batch.setExpireTime(LocalDateTime.now().plusMonths(12));
        pointsLogMapper.insert(batch);
    }

    /**
     * 消费积分：按批次先进先出扣减剩余额度。
     * 调用方必须先完成 member.points 的条件扣减（保证余额不超扣），本方法同一事务内维护批次。
     */
    public void deduct(Long memberId, int points, String remark) {
        if (points <= 0) {
            return;
        }
        List<UmsPointsLog> batches = pointsLogMapper.selectList(new LambdaQueryWrapper<UmsPointsLog>()
                .eq(UmsPointsLog::getMemberId, memberId)
                .eq(UmsPointsLog::getType, 1)
                .eq(UmsPointsLog::getStatus, 0)
                .gt(UmsPointsLog::getAmount, 0)
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
            log.warn("积分批次不足：会员 {} 扣减 {} 积分，批次剩余缺口 {}", memberId, points, remaining);
        }
        UmsPointsLog spend = new UmsPointsLog();
        spend.setMemberId(memberId);
        spend.setAmount(-points);
        spend.setType(2);
        spend.setRemark(remark);
        spend.setStatus(0);
        pointsLogMapper.insert(spend);
    }

    /**
     * 过期核销：将到期批次的剩余积分从会员余额中扣除并标记批次过期。
     *
     * @return 本次核销的批次数
     */
    @Transactional
    public int expireBatches(int limit) {
        List<UmsPointsLog> expired = pointsLogMapper.selectList(new LambdaQueryWrapper<UmsPointsLog>()
                .eq(UmsPointsLog::getType, 1)
                .eq(UmsPointsLog::getStatus, 0)
                .gt(UmsPointsLog::getAmount, 0)
                .le(UmsPointsLog::getExpireTime, LocalDateTime.now())
                .last("LIMIT " + limit));
        int handled = 0;
        for (UmsPointsLog batch : expired) {
            int amount = batch.getAmount();
            int affected = memberMapper.update(null, new LambdaUpdateWrapper<UmsMember>()
                    .setSql("points = points - " + amount)
                    .eq(UmsMember::getId, batch.getMemberId())
                    .ge(UmsMember::getPoints, amount));
            if (affected == 0) {
                log.warn("积分过期核销跳过（余额不足，数据异常）：batchId={} memberId={} amount={}",
                        batch.getId(), batch.getMemberId(), amount);
                continue;
            }
            pointsLogMapper.update(null, new LambdaUpdateWrapper<UmsPointsLog>()
                    .set(UmsPointsLog::getAmount, 0)
                    .set(UmsPointsLog::getStatus, 1)
                    .eq(UmsPointsLog::getId, batch.getId()));
            UmsPointsLog expireLog = new UmsPointsLog();
            expireLog.setMemberId(batch.getMemberId());
            expireLog.setAmount(-amount);
            expireLog.setType(3);
            expireLog.setRemark("积分过期（批次 #" + batch.getId() + "）");
            expireLog.setStatus(1);
            pointsLogMapper.insert(expireLog);
            handled++;
        }
        return handled;
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
