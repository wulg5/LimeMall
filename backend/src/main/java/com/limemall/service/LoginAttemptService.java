package com.limemall.service;

import com.limemall.common.BusinessException;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败限流器（内存实现）。
 *
 * 规则：同一账号 10 分钟内连续失败 5 次即锁定，锁定期内拒绝登录尝试。
 * 说明：单机内存实现满足课设/单实例部署场景；多实例部署时应替换为
 * Redis 计数（同一接口与语义）。
 */
@Component
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MILLIS = 10 * 60 * 1000L;

    private final Map<String, Deque<Long>> attempts = new ConcurrentHashMap<>();

    /** 登录前检查：是否已被锁定 */
    public void checkBlocked(String key) {
        Deque<Long> records = attempts.get(key);
        if (records == null) {
            return;
        }
        synchronized (records) {
            long threshold = System.currentTimeMillis() - WINDOW_MILLIS;
            while (!records.isEmpty() && records.peekFirst() < threshold) {
                records.pollFirst();
            }
            if (records.size() >= MAX_ATTEMPTS) {
                throw new BusinessException("尝试次数过多，账号已临时锁定，请 10 分钟后再试");
            }
        }
    }

    /** 当前窗口内失败次数（用于验证码门槛判断） */
    public int failureCount(String key) {
        Deque<Long> records = attempts.get(key);
        if (records == null) {
            return 0;
        }
        synchronized (records) {
            long threshold = System.currentTimeMillis() - WINDOW_MILLIS;
            while (!records.isEmpty() && records.peekFirst() < threshold) {
                records.pollFirst();
            }
            return records.size();
        }
    }

    /** 记录一次失败尝试 */
    public void recordFailure(String key) {
        Deque<Long> records = attempts.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (records) {
            records.addLast(System.currentTimeMillis());
        }
    }

    /** 登录成功后清除失败记录 */
    public void clear(String key) {
        attempts.remove(key);
    }
}
