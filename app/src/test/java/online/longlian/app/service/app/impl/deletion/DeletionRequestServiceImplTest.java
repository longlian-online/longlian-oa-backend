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
import online.longlian.common.enumeration.DeletionRequestStatus;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeletionRequestServiceImplTest {
    @Mock private DeletionRequestMapper requestMapper;
    @Mock private ProjectService projectService;
    @Mock private ItemService itemService;
    @Mock private UserService userService;
    private DeletionRequestServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), DeletionRequest.class);
        service = new DeletionRequestServiceImpl(requestMapper, projectService, itemService, userService,
                Clock.fixed(Instant.parse("2026-10-25T00:00:00Z"), ZoneOffset.UTC));
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
        verifyNoInteractions(projectService, itemService, userService);
    }
}
