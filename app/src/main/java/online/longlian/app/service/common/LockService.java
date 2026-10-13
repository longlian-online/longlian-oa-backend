package online.longlian.app.service.common;

import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.common.service.DistributedLockService;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class LockService {

    private final DistributedLockService distributedLockService;

    public DistributedLockService.Lock tryAcquireOrThrow(String key, long waitTime, long timeout, TimeUnit unit) {
        return require(distributedLockService.tryAcquire(key, waitTime, timeout, unit));
    }

    /**
     * 获取由 Redisson watchdog 续约的锁。事务时长不可预估时使用，避免固定租约先于事务结束。
     */
    public DistributedLockService.Lock tryAcquireOrThrow(String key, long waitTime, TimeUnit unit) {
        return require(distributedLockService.tryAcquire(key, waitTime, unit));
    }

    private DistributedLockService.Lock require(DistributedLockService.Lock lock) {
        if (lock == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "操作过于频繁，请稍后再试");
        }
        return lock;
    }
}
