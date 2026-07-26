package online.longlian.app.service.common;

import online.longlian.app.common.exception.AppException;
import online.longlian.common.service.DistributedLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LockServiceTest {

    @Mock
    private DistributedLockService distributedLockService;
    @Mock
    private DistributedLockService.Lock lock;

    private LockService lockService;

    @BeforeEach
    void setUp() {
        lockService = new LockService(distributedLockService);
    }

    @Test
    void tryAcquireOrThrow_lockAcquired_returnsLock() {
        when(distributedLockService.tryAcquire("key", 1, 5, TimeUnit.SECONDS)).thenReturn(lock);

        DistributedLockService.Lock result = lockService.tryAcquireOrThrow("key", 1, 5, TimeUnit.SECONDS);

        assertThat(result).isEqualTo(lock);
    }

    @Test
    void tryAcquireOrThrow_lockFailed_throws() {
        when(distributedLockService.tryAcquire("key", 0, 5, TimeUnit.SECONDS)).thenReturn(null);

        assertThatThrownBy(() -> lockService.tryAcquireOrThrow("key", 0, 5, TimeUnit.SECONDS))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("操作过于频繁");
    }
}
