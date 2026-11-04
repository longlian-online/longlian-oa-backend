package online.longlian.app.service.app.impl.project;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ProjectAttachmentMapper;
import online.longlian.app.pojo.bo.common.ResourceAbandonParamsBO;
import online.longlian.app.pojo.entity.ProjectAttachment;
import online.longlian.app.service.resource.ResourceService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectAttachmentHandlerTest {

    @Mock
    private ProjectAttachmentMapper projectAttachmentMapper;
    @Mock
    private ResourceService resourceService;

    private ProjectAttachmentHandler handler;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), ProjectAttachment.class);
        handler = new ProjectAttachmentHandler(projectAttachmentMapper, resourceService,
                Clock.fixed(Instant.parse("2026-05-01T02:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void abandonAttachment_softDeletesAssociationAndSendsFileToRetention() {
        when(projectAttachmentMapper.update(isNull(), any())).thenReturn(1);

        handler.abandonAttachment(attachment(200L, 20L), 1L);

        verify(projectAttachmentMapper).update(isNull(), any());
        ArgumentCaptor<ResourceAbandonParamsBO> abandoned = ArgumentCaptor.forClass(ResourceAbandonParamsBO.class);
        verify(resourceService).abandonBound(abandoned.capture());
        assertThat(abandoned.getValue().getOrgId()).isEqualTo(1L);
        assertThat(abandoned.getValue().getResourceIds()).containsExactly(20L);
    }

    @Test
    void abandonAttachment_alreadyDeletedThrowsWithoutTouchingResource() {
        when(projectAttachmentMapper.update(isNull(), any())).thenReturn(0);

        assertThatThrownBy(() -> handler.abandonAttachment(attachment(200L, 20L), 1L))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getCode()).isEqualTo(ResultCode.OPERATION_FAIL.getCode()));

        verifyNoInteractions(resourceService);
    }

    @Test
    void abandonProjectAttachments_withoutAttachmentsDoesNothing() {
        when(projectAttachmentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        handler.abandonProjectAttachments(100L, 1L);

        verify(projectAttachmentMapper, never()).update(isNull(), any());
        verifyNoInteractions(resourceService);
    }

    @Test
    void abandonProjectAttachments_softDeletesAllAndAbandonsDistinctFiles() {
        when(projectAttachmentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                attachment(200L, 20L),
                attachment(201L, 20L),
                attachment(202L, 21L)));
        when(projectAttachmentMapper.update(isNull(), any())).thenReturn(3);

        handler.abandonProjectAttachments(100L, 1L);

        ArgumentCaptor<ResourceAbandonParamsBO> abandoned = ArgumentCaptor.forClass(ResourceAbandonParamsBO.class);
        verify(resourceService).abandonBound(abandoned.capture());
        assertThat(abandoned.getValue().getResourceIds()).containsExactly(20L, 21L);
    }

    private ProjectAttachment attachment(Long id, Long fileId) {
        return ProjectAttachment.builder().id(id).projectId(100L).fileId(fileId).uploaderId(2L).build();
    }
}
