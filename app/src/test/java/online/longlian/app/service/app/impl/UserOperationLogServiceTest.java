package online.longlian.app.service.app.impl;

import com.alibaba.fastjson2.JSON;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.mapper.UserOperationLogMapper;
import online.longlian.app.pojo.entity.UserOperationLog;
import online.longlian.common.enumeration.UserOperationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserOperationLogServiceTest {

    @Mock
    private UserOperationLogMapper mapper;

    private UserOperationLogService service;

    @BeforeEach
    void setUp() {
        service = new UserOperationLogService(mapper,
                Clock.fixed(Instant.parse("2026-10-19T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void shouldPreserveOwnerCancelAuditIdentityAndRevisionWithoutReason() {
        when(mapper.insert(any(UserOperationLog.class))).thenReturn(1);
        Map<String, Object> body = Map.of("action", "OWNER_CANCEL", "instanceId", 41L,
                "assigneeId", 9L, "expectedRevision", "9007199254740993",
                "revision", "9007199254740994");

        service.logSynchronously(7L, 2L, 3L, UserOperationType.TASK_ABANDON, body);

        ArgumentCaptor<UserOperationLog> audit = ArgumentCaptor.forClass(UserOperationLog.class);
        verify(mapper).insert(audit.capture());
        UserOperationLog row = audit.getValue();
        assertThat(row.getUserId()).isEqualTo(7L);
        assertThat(row.getProjectId()).isEqualTo(2L);
        assertThat(row.getItemId()).isEqualTo(3L);
        assertThat(row.getOperationType()).isEqualTo(UserOperationType.TASK_ABANDON);
        var request = JSON.parseObject(row.getRequestBody());
        assertThat(request.getLong("instanceId")).isEqualTo(41L);
        assertThat(request.getLong("assigneeId")).isEqualTo(9L);
        assertThat(request).containsEntry("action", "OWNER_CANCEL")
                .containsEntry("expectedRevision", "9007199254740993")
                .containsEntry("revision", "9007199254740994");
        assertThat(request).doesNotContainKeys("reason", "reviewComment");
        assertThat(row.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 10, 19, 0, 0));
        assertThat(row.getUpdatedAt()).isEqualTo(row.getCreatedAt());
    }

    @Test
    void shouldFailOwnerOperationWhenAuditInsertDoesNotPersistExactlyOneRow() {
        when(mapper.insert(any(UserOperationLog.class))).thenReturn(0);

        assertThatThrownBy(() -> service.logSynchronously(7L, 2L, 3L,
                UserOperationType.TASK_REJECT, Map.of("action", "OWNER_REJECT", "reviewComment", "重做")))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("操作审计写入失败");
    }

    @Test
    void shouldPropagateAuditDatabaseFailureInsteadOfSwallowingIt() {
        IllegalStateException failure = new IllegalStateException("audit unavailable");
        when(mapper.insert(any(UserOperationLog.class))).thenThrow(failure);

        assertThatThrownBy(() -> service.logSynchronously(7L, 2L, 3L,
                UserOperationType.TASK_REJECT, Map.of("action", "OWNER_REJECT")))
                .isSameAs(failure);
    }
}
