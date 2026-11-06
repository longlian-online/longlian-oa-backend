package online.longlian.app.service.app.impl.project;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ProjectAttachmentMapper;
import online.longlian.app.pojo.entity.ProjectAttachment;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectAttachmentHandlerTest {

    @Mock
    private ProjectAttachmentMapper projectAttachmentMapper;

    private ProjectAttachmentHandler handler;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), ProjectAttachment.class);
        handler = new ProjectAttachmentHandler(projectAttachmentMapper,
                Clock.fixed(Instant.parse("2026-05-01T02:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void deleteAttachment_softDeletesAssociation() {
        when(projectAttachmentMapper.update(isNull(), any())).thenReturn(1);

        handler.deleteAttachment(attachment(200L, 20L));

        verify(projectAttachmentMapper).update(isNull(), any());
    }

    @Test
    void deleteAttachment_alreadyDeletedThrows() {
        when(projectAttachmentMapper.update(isNull(), any())).thenReturn(0);

        assertThatThrownBy(() -> handler.deleteAttachment(attachment(200L, 20L)))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getCode()).isEqualTo(ResultCode.OPERATION_FAIL.getCode()));
    }

    @Test
    void deleteProjectAttachments_withoutAttachmentsDoesNothing() {
        when(projectAttachmentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        handler.deleteProjectAttachments(100L);

        verify(projectAttachmentMapper, never()).update(isNull(), any());
    }

    @Test
    void deleteProjectAttachments_softDeletesAll() {
        when(projectAttachmentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                attachment(200L, 20L),
                attachment(201L, 20L),
                attachment(202L, 21L)));
        when(projectAttachmentMapper.update(isNull(), any())).thenReturn(3);

        handler.deleteProjectAttachments(100L);

        verify(projectAttachmentMapper).update(isNull(), any());
    }

    private ProjectAttachment attachment(Long id, Long fileId) {
        return ProjectAttachment.builder().id(id).projectId(100L).fileId(fileId).uploaderId(2L).build();
    }
}
