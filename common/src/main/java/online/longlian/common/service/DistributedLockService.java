package online.longlian.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private static final String LOCK_PREFIX = "longlian:lock:";

    private final RedissonClient redissonClient;

    /**
     * 获取分布式锁，调用方必须在 try 块中使用，确保最终会调用 Lock.close() 释放锁。
     * </br>
     * <strong>注意：</strong>此方法会一直等待直到获取到锁，可能导致线程阻塞。调用方应优先考虑使用 tryAcquire 方法。
     * @param key 锁的唯一标识，建议使用业务相关的前缀以便排查问题，例如 "scheduledTask:taskName"。
     * @param timeout 锁的自动释放时间，防止死锁。调用方也可以通过 Lock.close() 提前释放锁。
     * @param unit 时间单位
     * @return 成功获取锁返回 Lock 对象，失败（例如被其他线程持有）返回 null。
     */
    public Lock acquire(String key, long timeout, TimeUnit unit) {
        RLock rlock = redissonClient.getLock(LOCK_PREFIX + key);
        rlock.lock(timeout, unit);
        return new Lock(rlock);
    }

    /**
     * 尝试获取分布式锁，调用方必须在 try 块中使用，确保最终会调用 Lock.close() 释放锁。
     * @param key 锁的唯一标识，建议使用业务相关的前缀以便排查问题，例如 "scheduledTask:taskName"。
     * @param waitTime 等待获取锁的时间，超过此时间仍未获取到锁则返回 null。
     * @param timeout 锁的自动释放时间，防止死锁。调用方也可以通过 Lock.close() 提前释放锁。
     * @param unit 时间单位
     * @return 成功获取锁返回 Lock 对象，失败（例如被其他线程持有）返回 null。
     */
    public Lock tryAcquire(String key, long waitTime, long timeout, TimeUnit unit) {
        RLock rlock = redissonClient.getLock(LOCK_PREFIX + key);
        try {
            if (rlock.tryLock(waitTime, timeout, unit)) {
                return new Lock(rlock);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    /**
     * 分布式锁的封装类，实现了 AutoCloseable 接口，便于在 try-with-resources 语句中使用。
     */
    public class Lock implements AutoCloseable {

        private final RLock rlock;

        Lock(RLock rlock) {
            this.rlock = rlock;
        }

        @Override
        public void close() {
            if (rlock.isHeldByCurrentThread()) {
                rlock.unlock();
            }
        }
    }
}