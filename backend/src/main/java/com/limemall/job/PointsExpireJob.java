package com.limemall.job;

import com.limemall.service.PointsLedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 积分过期定时任务：启动时执行一次补跑，此后每日 03:40 全量扫描。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointsExpireJob {

    private final PointsLedgerService pointsLedgerService;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        run();
    }

    @Scheduled(cron = "0 40 3 * * ?")
    public void daily() {
        run();
    }

    private void run() {
        try {
            LocalDateTime cutoff = LocalDateTime.now();
            long afterId = 0;
            int handled = 0;
            List<Long> batchIds = pointsLedgerService.expiredBatchIdsAfter(afterId, cutoff);
            while (!batchIds.isEmpty()) {
                for (Long batchId : batchIds) {
                    if (pointsLedgerService.expireBatch(batchId, cutoff)) {
                        handled++;
                    }
                }
                afterId = batchIds.getLast();
                batchIds = pointsLedgerService.expiredBatchIdsAfter(afterId, cutoff);
            }
            if (handled > 0) {
                log.info("积分过期核销完成：{} 笔批次", handled);
            }
        } catch (Exception e) {
            log.error("积分过期任务执行失败", e);
        }
    }
}
