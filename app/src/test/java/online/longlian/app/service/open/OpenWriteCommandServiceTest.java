package online.longlian.app.service.open;

import online.longlian.app.common.exception.OpenAppException;
import online.longlian.app.common.result.OpenResultCode;
import online.longlian.app.pojo.bo.open.IdempotencyCheckParamsBO;
import online.longlian.app.pojo.bo.open.IdempotencyCheckResultBO;
import online.longlian.app.pojo.bo.open.IdempotencyCheckOutcome;
import online.longlian.app.pojo.bo.open.IdempotencyRecordParamsBO;
import online.longlian.common.enumeration.IdempotencySource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenWriteCommandServiceTest {

    private IdempotencyService idempotencyService;
    private OpenWriteCommandService service;

    @BeforeEach
    void setUp() {
        idempotencyService = mock(IdempotencyService.class);
        service = new OpenWriteCommandService(idempotencyService);
    }

    private IdempotencyCheckParamsBO checkParams() {
        return new IdempotencyCheckParamsBO(IdempotencySource.OPEN_API, 5L, "uuid-1",
                "POST", "/open/v1/tasks/1/claim", "hash-1");
    }

    /** HIT：直接回放首次快照，业务不重复执行，也不再写幂等记录。 */
    @Test
    void shouldReplaySnapshotWithoutRerunningBusinessOnHit() {
        Object snapshot = Map.of("autoJoinedProject", true);
        when(idempotencyService.check(any())).thenReturn(new IdempotencyCheckResultBO(IdempotencyCheckOutcome.HIT, snapshot));
        AtomicInteger executions = new AtomicInteger();

        Object result = service.execute(checkParams(), executions::incrementAndGet);

        assertThat(result).isEqualTo(snapshot);
        assertThat(executions.get()).isZero();
        verify(idempotencyService, never()).record(any());
    }

    /** 快照超限的 HIT：data 为 null，同样不重复执行业务。 */
    @Test
    void shouldReturnNullDataForTruncatedSnapshotHit() {
        when(idempotencyService.check(any())).thenReturn(new IdempotencyCheckResultBO(IdempotencyCheckOutcome.HIT, null));
        AtomicInteger executions = new AtomicInteger();

        Object result = service.execute(checkParams(), executions::incrementAndGet);

        assertThat(result).isNull();
        assertThat(executions.get()).isZero();
    }

    /** CONFLICT：同键不同请求体，拒绝执行业务，也不写记录。 */
    @Test
    void shouldRejectBusinessOnConflict() {
        when(idempotencyService.check(any())).thenReturn(new IdempotencyCheckResultBO(IdempotencyCheckOutcome.CONFLICT, null));
        AtomicInteger executions = new AtomicInteger();

        assertThatThrownBy(() -> service.execute(checkParams(), executions::incrementAndGet))
                .isInstanceOf(OpenAppException.class)
                .hasMessageContaining("幂等键冲突")
                .extracting("code")
                .isEqualTo(OpenResultCode.IDEMPOTENCY_CONFLICT.getCode());
        assertThat(executions.get()).isZero();
        verify(idempotencyService, never()).record(any());
    }

    /** MISS：执行业务一次，并把业务结果作为快照写入同事务幂等记录。 */
    @Test
    void shouldRunBusinessAndRecordSnapshotOnMiss() {
        when(idempotencyService.check(any())).thenReturn(new IdempotencyCheckResultBO(IdempotencyCheckOutcome.MISS, null));
        Object data = Map.of("versionNo", 1);

        Object result = service.execute(checkParams(), () -> data);

        assertThat(result).isEqualTo(data);
        ArgumentCaptor<IdempotencyRecordParamsBO> captor =
                ArgumentCaptor.forClass(IdempotencyRecordParamsBO.class);
        verify(idempotencyService).record(captor.capture());
        IdempotencyRecordParamsBO recorded = captor.getValue();
        assertThat(recorded.source()).isEqualTo(IdempotencySource.OPEN_API);
        assertThat(recorded.callerId()).isEqualTo(5L);
        assertThat(recorded.idempotencyKey()).isEqualTo("uuid-1");
        assertThat(recorded.method()).isEqualTo("POST");
        assertThat(recorded.path()).isEqualTo("/open/v1/tasks/1/claim");
        assertThat(recorded.bodyHash()).isEqualTo("hash-1");
        assertThat(recorded.resultData()).isEqualTo(data);
    }

    /** 业务返回 null（如放弃任务）时仍写幂等记录，快照为空只代表无业务数据。 */
    @Test
    void shouldRecordMissEvenWhenBusinessReturnsNull() {
        when(idempotencyService.check(any())).thenReturn(new IdempotencyCheckResultBO(IdempotencyCheckOutcome.MISS, null));

        Object result = service.execute(checkParams(), () -> null);

        assertThat(result).isNull();
        ArgumentCaptor<IdempotencyRecordParamsBO> captor =
                ArgumentCaptor.forClass(IdempotencyRecordParamsBO.class);
        verify(idempotencyService).record(captor.capture());
        assertThat(captor.getValue().resultData()).isNull();
    }

    /** 业务抛异常时不写幂等记录，调用方可换新键原样重试。 */
    @Test
    void shouldNotRecordWhenBusinessFails() {
        when(idempotencyService.check(any())).thenReturn(new IdempotencyCheckResultBO(IdempotencyCheckOutcome.MISS, null));

        assertThatThrownBy(() -> service.execute(checkParams(), () -> {
            throw new OpenAppException(OpenResultCode.PARAM_ERROR, "该任务不可接取");
        })).isInstanceOf(OpenAppException.class);

        verify(idempotencyService, never()).record(any());
    }
}
