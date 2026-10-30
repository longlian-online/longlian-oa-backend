package online.longlian.app.service.outbox;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import online.longlian.app.mapper.OutboxMessageMapper;
import online.longlian.app.pojo.entity.OutboxMessage;
import online.longlian.common.enumeration.OutboxMessageStatus;
import online.longlian.common.enumeration.OutboxMessageType;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxDispatcherTest {
    private static final LocalDateTime EXECUTE_TIME = LocalDateTime.of(2026, 10, 23, 10, 0);
    private static final Pattern ASSIGNMENT = Pattern.compile("([\\w`]+)=#\\{ew\\.paramNameValuePairs\\.(\\w+)}");

    OutboxMessageMapper mapper;
    OutboxHandler handler;

    @BeforeAll
    static void initTable() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), OutboxMessage.class);
    }

    @BeforeEach
    void setUp() {
        mapper = mock(OutboxMessageMapper.class);
        handler = mock(OutboxHandler.class);
        when(handler.type()).thenReturn(OutboxMessageType.INBOX_EMAIL);
    }

    @Test
    void shouldMarkDuePendingMessageSent() {
        OutboxMessage message = row(1L, OutboxMessageStatus.PENDING, 0, EXECUTE_TIME.minusMinutes(1));
        stubPage(List.of(message));
        when(mapper.update(isNull(), any())).thenReturn(1);

        dispatcher().dispatch(EXECUTE_TIME);

        List<LambdaUpdateWrapper<OutboxMessage>> updates = updates();
        assertThat(assignments(updates.get(0))).containsEntry("status", OutboxMessageStatus.PROCESSING)
                .containsEntry("attempt_count", 1)
                .containsEntry("locked_until", EXECUTE_TIME.plusMinutes(10))
                .containsEntry("updated_at", EXECUTE_TIME);
        assertThat(assignments(updates.get(1))).containsEntry("status", OutboxMessageStatus.SENT)
                .containsEntry("locked_until", null)
                .containsEntry("last_error", null)
                .doesNotContainKey("available_at");
        verify(handler).deliver(message, EXECUTE_TIME);
    }

    @Test
    void shouldLeaveSkipTerminalAndNotMoveAvailableAt() {
        stubPage(List.of(row(2L, OutboxMessageStatus.PENDING, 0, EXECUTE_TIME)));
        when(mapper.update(isNull(), any())).thenReturn(1);
        doThrow(new OutboxSkipException("来源不可用")).when(handler).deliver(any(), any());

        dispatcher().dispatch(EXECUTE_TIME);

        assertThat(assignments(updates().get(1))).containsEntry("status", OutboxMessageStatus.SKIPPED)
                .containsEntry("last_error", "来源不可用")
                .doesNotContainKey("available_at");
    }

    @ParameterizedTest
    @CsvSource({"0,1", "1,5", "2,15", "3,60"})
    void shouldReturnRetryableFailureToPendingWithBackoff(int attemptsBeforeClaim, int delayMinutes) {
        stubPage(List.of(row(3L, OutboxMessageStatus.PENDING, attemptsBeforeClaim, EXECUTE_TIME)));
        when(mapper.update(isNull(), any())).thenReturn(1);
        doThrow(new IllegalStateException("smtp down")).when(handler).deliver(any(), any());

        dispatcher().dispatch(EXECUTE_TIME);

        assertThat(assignments(updates().get(1))).containsEntry("status", OutboxMessageStatus.PENDING)
                .containsEntry("available_at", EXECUTE_TIME.plusMinutes(delayMinutes))
                .containsEntry("locked_until", null)
                .containsEntry("last_error", "smtp down");
    }

    @Test
    void shouldFailOnTheFifthAttempt() {
        stubPage(List.of(row(4L, OutboxMessageStatus.PENDING, 4, EXECUTE_TIME)));
        when(mapper.update(isNull(), any())).thenReturn(1);
        doThrow(new IllegalStateException("smtp down")).when(handler).deliver(any(), any());

        dispatcher().dispatch(EXECUTE_TIME);

        Map<String, Object> finish = assignments(updates().get(1));
        assertThat(finish).containsEntry("status", OutboxMessageStatus.FAILED)
                .containsEntry("last_error", "smtp down")
                .doesNotContainKey("available_at");
        assertThat(assignments(updates().get(0))).containsEntry("attempt_count", 5);
    }

    @Test
    void shouldSelectPendingAndExpiredProcessingOnly() {
        when(mapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<OutboxMessage> page = invocation.getArgument(0);
            page.setRecords(List.of());
            return page;
        });

        dispatcher().dispatch(EXECUTE_TIME);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Page<OutboxMessage>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OutboxMessage>> wrapperCaptor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);
        verify(mapper).selectPage(pageCaptor.capture(), wrapperCaptor.capture());
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(20);
        assertThat(pageCaptor.getValue().searchCount()).isFalse();
        String sql = wrapperCaptor.getValue().getSqlSegment();
        assertThat(sql).contains("available_at <=").contains("locked_until <=")
                .contains("ORDER BY id ASC").doesNotContain("FOR UPDATE").doesNotContainIgnoringCase("limit");
        assertThat(wrapperCaptor.getValue().getParamNameValuePairs().values())
                .contains(OutboxMessageStatus.PENDING, EXECUTE_TIME, OutboxMessageStatus.PROCESSING);
    }

    @Test
    void shouldClaimExpiredProcessingAgain() {
        OutboxMessage message = row(5L, OutboxMessageStatus.PROCESSING, 2, EXECUTE_TIME.minusMinutes(30));
        message.setLockedUntil(EXECUTE_TIME.minusMinutes(1));
        stubPage(List.of(message));
        when(mapper.update(isNull(), any())).thenReturn(1);

        dispatcher().dispatch(EXECUTE_TIME);

        assertThat(assignments(updates().get(0))).containsEntry("status", OutboxMessageStatus.PROCESSING)
                .containsEntry("attempt_count", 3)
                .containsEntry("locked_until", EXECUTE_TIME.plusMinutes(10));
        assertThat(updates().get(0).getSqlSegment()).contains("status");
        assertThat(updates().get(0).getParamNameValuePairs().values())
                .contains(OutboxMessageStatus.PROCESSING, 2);
        assertThat(assignments(updates().get(1))).containsEntry("status", OutboxMessageStatus.SENT);
    }

    @Test
    void shouldSkipWhenClaimLosesTheRace() {
        stubPage(List.of(row(6L, OutboxMessageStatus.PENDING, 0, EXECUTE_TIME)));
        when(mapper.update(isNull(), any())).thenReturn(0);

        dispatcher().dispatch(EXECUTE_TIME);

        verify(handler, never()).deliver(any(), any());
    }

    @Test
    void shouldGiveUpWhenFinishMatchesNoRow() {
        stubPage(List.of(row(7L, OutboxMessageStatus.PENDING, 0, EXECUTE_TIME)));
        when(mapper.update(isNull(), any())).thenReturn(1, 0);

        dispatcher().dispatch(EXECUTE_TIME);

        verify(handler).deliver(any(), any());
    }

    @Test
    void shouldContinueAfterOneMessageFails() {
        stubPage(List.of(
                row(8L, OutboxMessageStatus.PENDING, 0, EXECUTE_TIME),
                row(9L, OutboxMessageStatus.PENDING, 1, EXECUTE_TIME)));
        when(mapper.update(isNull(), any())).thenReturn(1);
        doThrow(new IllegalStateException("smtp down")).doNothing().when(handler).deliver(any(), any());

        dispatcher().dispatch(EXECUTE_TIME);

        List<Map<String, Object>> finishes = updates().stream().skip(0).map(OutboxDispatcherTest::assignments)
                .filter(values -> values.containsKey("last_error") || values.get("status") == OutboxMessageStatus.SENT)
                .toList();
        assertThat(finishes).hasSize(2);
        assertThat(finishes.get(0)).containsEntry("status", OutboxMessageStatus.PENDING)
                .containsEntry("available_at", EXECUTE_TIME.plusMinutes(1));
        assertThat(finishes.get(1)).containsEntry("status", OutboxMessageStatus.SENT);
    }

    @Test
    void shouldFailTheTaskWhenTheQueryFails() {
        when(mapper.selectPage(any(), any())).thenThrow(new IllegalStateException("db down"));
        assertThatThrownBy(() -> dispatcher().dispatch(EXECUTE_TIME))
                .isInstanceOf(IllegalStateException.class).hasMessage("db down");
    }

    @Test
    void shouldFailPermanentlyAndTruncateErrorsWithoutUsingAClock() {
        stubPage(List.of(row(10L, OutboxMessageStatus.PENDING, 0, EXECUTE_TIME)));
        when(mapper.update(isNull(), any())).thenReturn(1);
        doThrow(new OutboxPermanentException("x".repeat(600))).when(handler).deliver(any(), any());

        dispatcher().dispatch(EXECUTE_TIME);

        assertThat(assignments(updates().get(1))).containsEntry("status", OutboxMessageStatus.FAILED)
                .containsEntry("last_error", "x".repeat(500))
                .doesNotContainKey("available_at");
    }

    @Test
    void shouldUseExceptionClassNameWhenMessageIsNull() {
        stubPage(List.of(row(11L, OutboxMessageStatus.PENDING, 0, EXECUTE_TIME)));
        when(mapper.update(isNull(), any())).thenReturn(1);
        doThrow(new SilentFailureException()).when(handler).deliver(any(), any());

        dispatcher().dispatch(EXECUTE_TIME);

        assertThat(assignments(updates().get(1))).containsEntry("last_error", "SilentFailureException");
    }

    @Test
    void shouldFailWhenNoHandlerMatchesTheMessageType() {
        stubPage(List.of(row(12L, null, 0, EXECUTE_TIME)));
        when(mapper.update(isNull(), any())).thenReturn(1);

        dispatcher().dispatch(EXECUTE_TIME);

        verify(handler, never()).deliver(any(), any());
        assertThat(assignments(updates().get(1))).containsEntry("status", OutboxMessageStatus.FAILED)
                .containsEntry("last_error", "没有对应的 outbox 处理器");
    }

    @Test
    void shouldContinueWhenClaimingOneMessageThrows() {
        stubPage(List.of(row(13L, OutboxMessageStatus.PENDING, 0, EXECUTE_TIME)));
        when(mapper.update(isNull(), any())).thenThrow(new IllegalStateException("claim failed"));

        dispatcher().dispatch(EXECUTE_TIME);

        verify(handler, never()).deliver(any(), any());
    }

    @Test
    void shouldUseBinaryNameWhenExceptionHasNoSimpleName() {
        stubPage(List.of(row(14L, OutboxMessageStatus.PENDING, 0, EXECUTE_TIME)));
        when(mapper.update(isNull(), any())).thenReturn(1);
        doThrow(new RuntimeException((String) null) {
        }).when(handler).deliver(any(), any());

        dispatcher().dispatch(EXECUTE_TIME);

        assertThat(assignments(updates().get(1)).get("last_error").toString()).contains("OutboxDispatcherTest");
    }

    @Test
    void shouldRejectHandlerWithoutType() {
        OutboxHandler untyped = mock(OutboxHandler.class);
        when(untyped.type()).thenReturn(null);
        assertThatThrownBy(() -> new OutboxDispatcher(List.of(untyped), mapper, transactionManager()))
                .isInstanceOf(IllegalStateException.class).hasMessage("outbox 处理器缺少类型");
    }

    @Test
    void shouldRejectDuplicateOrMissingHandlersAtConstruction() {
        OutboxHandler another = mock(OutboxHandler.class);
        when(another.type()).thenReturn(OutboxMessageType.INBOX_EMAIL);
        assertThatThrownBy(() -> new OutboxDispatcher(List.of(handler, another), mapper, transactionManager()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("多个处理器");
        assertThatThrownBy(() -> new OutboxDispatcher(List.of(), mapper, transactionManager()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("没有处理器");
    }

    private OutboxDispatcher dispatcher() {
        OutboxHandler webhook = mock(OutboxHandler.class);
        when(webhook.type()).thenReturn(OutboxMessageType.TASK_WEBHOOK);
        return new OutboxDispatcher(List.of(handler, webhook), mapper, transactionManager());
    }

    private void stubPage(List<OutboxMessage> records) {
        when(mapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<OutboxMessage> page = invocation.getArgument(0);
            page.setRecords(records);
            return page;
        });
    }

    private List<LambdaUpdateWrapper<OutboxMessage>> updates() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaUpdateWrapper<OutboxMessage>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(mapper, org.mockito.Mockito.atLeastOnce()).update(isNull(), captor.capture());
        return captor.getAllValues();
    }

    private static Map<String, Object> assignments(LambdaUpdateWrapper<?> wrapper) {
        Map<String, Object> params = wrapper.getParamNameValuePairs();
        Map<String, Object> values = new LinkedHashMap<>();
        Matcher matcher = ASSIGNMENT.matcher(wrapper.getSqlSet());
        while (matcher.find()) {
            values.put(matcher.group(1).replace("`", ""), params.get(matcher.group(2)));
        }
        return values;
    }

    private static OutboxMessage row(long id, OutboxMessageStatus status, int attempts, LocalDateTime availableAt) {
        return OutboxMessage.builder().id(id).type(status == null ? null : OutboxMessageType.INBOX_EMAIL)
                .status(status).attemptCount(attempts).availableAt(availableAt).build();
    }

    private static org.springframework.transaction.PlatformTransactionManager transactionManager() {
        return new org.springframework.transaction.support.AbstractPlatformTransactionManager() {
            @Override
            protected Object doGetTransaction() {
                return new Object();
            }

            @Override
            protected void doBegin(Object transaction, org.springframework.transaction.TransactionDefinition definition) {
            }

            @Override
            protected void doCommit(org.springframework.transaction.support.DefaultTransactionStatus status) {
            }

            @Override
            protected void doRollback(org.springframework.transaction.support.DefaultTransactionStatus status) {
            }
        };
    }

    private static final class SilentFailureException extends RuntimeException {
        private SilentFailureException() {
            super((String) null);
        }
    }

}
