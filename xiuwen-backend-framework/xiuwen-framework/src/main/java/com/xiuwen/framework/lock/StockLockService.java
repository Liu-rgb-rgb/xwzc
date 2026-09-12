package com.xiuwen.framework.lock;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 库存分布式锁。同一商品的扣减/回补必须串行化,避免并发导致超卖或库存被覆盖。
 * 多商品场景通过 RedissonMultiLock 一次性原子获取,并按 productId 排序防止死锁。
 */
@Component
@RequiredArgsConstructor
public class StockLockService {

    private static final String KEY_PREFIX = "lock:stock:product:";
    /** 最长等待时间(秒),拿不到锁直接抛业务异常,让用户重试 */
    private static final long DEFAULT_WAIT_SECONDS = 3L;
    /** 锁持有时间(秒),兜底防止业务卡死后死锁 */
    private static final long DEFAULT_LEASE_SECONDS = 30L;

    private final RedissonClient redissonClient;

    /**
     * 尝试获取给定商品集合的库存锁。返回已获取到的锁(可能是单锁或多锁);
     * 获取失败返回 null,调用方需要判空。
     */
    public RLock tryLockStock(Collection<Long> productIds) {
        List<Long> sortedIds = productIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        if (sortedIds.isEmpty()) {
            return null;
        }
        RLock lock;
        if (sortedIds.size() == 1) {
            lock = redissonClient.getLock(KEY_PREFIX + sortedIds.get(0));
        } else {
            RLock[] locks = sortedIds.stream()
                    .map(id -> redissonClient.getLock(KEY_PREFIX + id))
                    .toArray(RLock[]::new);
            lock = redissonClient.getMultiLock(locks);
        }
        try {
            boolean acquired = lock.tryLock(DEFAULT_WAIT_SECONDS, DEFAULT_LEASE_SECONDS, TimeUnit.SECONDS);
            return acquired ? lock : null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public void unlock(RLock lock) {
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
