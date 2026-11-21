package online.longlian.app.service.open;

import com.alibaba.fastjson2.JSONObject;
import online.longlian.app.common.exception.OpenAppException;
import online.longlian.app.common.result.OpenResultCode;
import online.longlian.app.mapper.IdempotencyRecordMapper;
import online.longlian.app.pojo.bo.open.IdempotencyCheckParamsBO;
import online.longlian.app.pojo.bo.open.IdempotencyCheckResultBO;
import online.longlian.app.pojo.bo.open.IdempotencyCheckOutcome;
import online.longlian.app.pojo.bo.open.IdempotencyRecordParamsBO;
import online.longlian.app.pojo.entity.IdempotencyRecord;
import online.longlian.common.enumeration.IdempotencySource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdempotencyServiceImplTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("Asia/Shanghai"));

    private IdempotencyRecordMapper mapper;
    private IdempotencyServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(IdempotencyRecordMapper.class);
        service = new IdempotencyServiceImpl(mapper, CLOCK);
    }

    private IdempotencyCheckParamsBO checkParams(String method, String path, String bodyHash) {
        return new IdempotencyCheckParamsBO(IdempotencySource.OPEN_API, 5L, "uuid-1", method, path, bodyHash);
    }

    private IdempotencyRecord record(String method, String path, String bodyHash, String snapshot) {
        return IdempotencyRecord.builder()
                .source(IdempotencySource.OPEN_API).callerId(5L).idempotencyKey("uuid-1")
                .method(method).path(path).bodyHash(bodyHash)
                .snapshot(snapshot).snapshotTruncated((byte) 0)
                .build();
    }

    @Test
    void shouldMissWhenNoRecordExists() {
        when(mapper.selectOne(any())).thenReturn(null);

        assertThat(service.check(checkParams("POST", "/open/v1/tasks/1/claim", "h")).outcome())
                .isEqualTo(IdempotencyCheckOutcome.MISS);
    }

    @Test
    void shouldHitAndReplaySnapshotWhenSameKeyMethodPathAndBody() {
        when(mapper.selectOne(any())).thenReturn(record("POST", "/open/v1/tasks/1/claim", "h", "{\"id\":7}"));

        IdempotencyCheckResultBO result = service.check(checkParams("POST", "/open/v1/tasks/1/claim", "h"));

        assertThat(result.outcome()).isEqualTo(IdempotencyCheckOutcome.HIT);
        assertThat(((JSONObject) result.data()).get("id")).isEqualTo(7);
    }

    @Test
    void shouldHitWithoutReplayWhenSnapshotTruncated() {
        when(mapper.selectOne(any())).thenReturn(record("POST", "/open/v1/tasks/1/claim", "h", null));

        IdempotencyCheckResultBO result = service.check(checkParams("POST", "/open/v1/tasks/1/claim", "h"));

        assertThat(result.outcome()).isEqualTo(IdempotencyCheckOutcome.HIT);
        assertThat(result.data()).isNull();
    }

    @Test
    void shouldConflictWhenRequestBodyDiffers() {
        when(mapper.selectOne(any())).thenReturn(record("POST", "/open/v1/tasks/1/claim", "h1", "{\"id\":7}"));

        assertThat(service.check(checkParams("POST", "/open/v1/tasks/1/claim", "h2")).outcome())
                .isEqualTo(IdempotencyCheckOutcome.CONFLICT);
    }

    @Test
    void shouldConflictWhenSameKeyUsedOnAnotherEndpoint() {
        when(mapper.selectOne(any())).thenReturn(record("POST", "/open/v1/tasks/1/claim", "h", "{\"id\":7}"));

        assertThat(service.check(checkParams("POST", "/open/v1/tasks/2/claim", "h")).outcome())
                .isEqualTo(IdempotencyCheckOutcome.CONFLICT);
        assertThat(service.check(checkParams("PUT", "/open/v1/tasks/1/claim", "h")).outcome())
                .isEqualTo(IdempotencyCheckOutcome.CONFLICT);
    }

    @Test
    void shouldRecordSnapshotInSameCall() {
        service.record(new IdempotencyRecordParamsBO(IdempotencySource.OPEN_API, 5L, "uuid-1",
                "POST", "/open/v1/tasks/1/submit", "h", Map.of("submissionId", 7)));

        ArgumentCaptor<IdempotencyRecord> captor = ArgumentCaptor.forClass(IdempotencyRecord.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getSnapshot()).contains("submissionId");
        assertThat(captor.getValue().getSnapshotTruncated()).isEqualTo((byte) 0);
        assertThat(captor.getValue().getSource()).isEqualTo(IdempotencySource.OPEN_API);
        assertThat(captor.getValue().getMethod()).isEqualTo("POST");
    }

    @Test
    void shouldDropSnapshotBodyWhenOver64KB() {
        String bigData = "x".repeat(65 * 1024);

        service.record(new IdempotencyRecordParamsBO(IdempotencySource.OPEN_API, 5L, "uuid-1",
                "POST", "/open/v1/tasks/1/submit", "h", bigData));

        ArgumentCaptor<IdempotencyRecord> captor = ArgumentCaptor.forClass(IdempotencyRecord.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getSnapshot()).isNull();
        assertThat(captor.getValue().getSnapshotTruncated()).isEqualTo((byte) 1);
    }

    @Test
    void shouldThrowIdempotencyConflictWhenConcurrentInsertDuplicates() {
        when(mapper.insert(any(IdempotencyRecord.class))).thenThrow(new DuplicateKeyException("dup"));

        assertThatThrownBy(() -> service.record(new IdempotencyRecordParamsBO(
                IdempotencySource.OPEN_API, 5L, "uuid-1", "POST", "/open/v1/tasks/1/submit", "h", Map.of())))
                .isInstanceOf(OpenAppException.class)
                .satisfies(e -> assertThat(((OpenAppException) e).getCode())
                        .isEqualTo(OpenResultCode.IDEMPOTENCY_CONFLICT.getCode()));
    }
}
