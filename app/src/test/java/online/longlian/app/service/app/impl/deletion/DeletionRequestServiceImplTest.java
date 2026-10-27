package online.longlian.app.service.app.impl.deletion;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.DeletionRequestMapper;
import online.longlian.app.pojo.entity.DeletionRequest;
import online.longlian.app.service.app.ItemService;
import online.longlian.app.service.app.ProjectService;
import online.longlian.app.service.app.UserService;
import online.longlian.app.service.app.impl.notification.AdminReviewNotificationPublisher;
import online.longlian.common.enumeration.DeletionRequestStatus;
import online.longlian.common.enumeration.DeletionTargetType;
import online.longlian.common.enumeration.InboxNotificationKind;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeletionRequestServiceImplTest {
    @Mock private DeletionRequestMapper requestMapper;
    @Mock private ProjectService projectService;
    @Mock private ItemService itemService;
    @Mock private UserService userService;
    @Mock private AdminReviewNotificationPublisher notifications;
    private DeletionRequestServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), DeletionRequest.class);
        service = new DeletionRequestServiceImpl(requestMapper, projectService, itemService, userService,
                Clock.fixed(Instant.parse("2026-10-25T00:00:00Z"), ZoneOffset.UTC), notifications);
    }

    @Test
    void review_rejectsNonTerminalDecisionWithoutReadingOrChangingRequests() {
        assertThatThrownBy(() -> service.review(1L, 1L, 2L, DeletionRequestStatus.PENDING))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getCode())
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
        verifyNoInteractions(requestMapper, projectService, itemService, userService);
    }

    @Test
    void review_rejectsALostPendingTransitionInsteadOfReportingSuccess() {
        when(requestMapper.selectById(1L)).thenReturn(DeletionRequest.builder()
                .id(1L).orgId(1L).status(DeletionRequestStatus.PENDING).build());
        when(requestMapper.update(isNull(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.review(1L, 1L, 2L, DeletionRequestStatus.REJECTED))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getCode())
                .isEqualTo(ResultCode.OPERATION_FAIL.getCode());
        verifyNoInteractions(projectService, itemService, userService, notifications);
    }

    @Test
    void createNotifiesEveryEnabledAdministratorForBothTargets() {
        when(projectService.requireCreatorProjectTitle(9L, 2L, 1L)).thenReturn("作品");
        when(itemService.hasProjectItems(9L)).thenReturn(true);
        when(itemService.requireItemTitle(9L, 8L)).thenReturn("第一章");
        when(itemService.hasSubmittedTask(8L)).thenReturn(true);
        when(requestMapper.insert(any(DeletionRequest.class))).thenAnswer(invocation -> {
            DeletionRequest request = invocation.getArgument(0);
            request.setId(request.getTargetType() == DeletionTargetType.PROJECT ? 55L : 56L);
            return 1;
        });

        service.createProjectRequest(9L, 2L, 1L, " 不再继续 ");
        service.createItemRequest(9L, 8L, 2L, 1L, " 录错了 ");

        verify(notifications).publish(eq(1L), eq(InboxNotificationKind.DELETION_REQUEST), eq(55L),
                eq(java.time.LocalDateTime.of(2026, 10, 25, 0, 0)));
        verify(notifications).publish(eq(1L), eq(InboxNotificationKind.DELETION_REQUEST), eq(56L),
                eq(java.time.LocalDateTime.of(2026, 10, 25, 0, 0)));
    }
}
