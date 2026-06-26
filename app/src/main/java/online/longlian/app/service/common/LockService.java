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
        DistributedLockService.Lock lock = distributedLockService.tryAcquire(key, waitTime, timeout, unit);
        if (lock == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "操作过于频繁，请稍后再试");
        }
        return lock;
    }
}
