package online.longlian.app.service.app.impl.notification;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.mapper.UserNotificationMapper;
import online.longlian.app.pojo.bo.app.*;
import online.longlian.app.pojo.bo.common.ResourceReadUrlGetResultBO;
import online.longlian.app.pojo.entity.UserNotification;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.InboxNotificationKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InboxNotificationServiceImplTest {
    @Mock UserNotificationMapper mapper;
    @Mock NotificationSourceResolver resolver;
    @Mock ResourceService resources;
    @Mock InboxEmailOutboxWriter writer;
    final Clock clock = Clock.fixed(Instant.parse("2026-10-22T12:00:00Z"), ZoneOffset.UTC);
    InboxNotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InboxNotificationServiceImpl(resolver, resources, clock, writer);
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), ""),
                UserNotification.class);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @Test
    void shouldKeepOriginalReadTimeOnRepeatedReadWithoutResolvingSource() {
        var original = LocalDateTime.of(2026,10,20,1,2,3);
        when(mapper.selectOne(any())).thenReturn(UserNotification.builder().id(7L).readAt(original).build());
        assertThat(service.markRead(new NotificationReadParamsBO(1L,2L,7L)).getReadAt()).isEqualTo(original);
        verify(mapper,never()).update(isNull(),any());
        verifyNoInteractions(resolver,resources);
    }

    @Test
    void shouldRejectUnavailableInboxRowWithoutResolvingSource() {
        when(mapper.selectOne(any())).thenReturn(null);
        assertThatThrownBy(() -> service.markRead(new NotificationReadParamsBO(1L,2L,7L)))
                .isInstanceOf(AppException.class).hasMessage("通知不存在");
        verify(mapper,never()).update(isNull(),any());
        verifyNoInteractions(resolver,resources);
    }

    @Test
    void shouldMarkUnreadRowEvenWhenSourceIsUnavailable() {
        when(mapper.selectOne(any())).thenReturn(UserNotification.builder().id(7L).build());
        assertThat(service.markRead(new NotificationReadParamsBO(1L,2L,7L)).getReadAt())
                .isEqualTo(LocalDateTime.now(clock));
        verifyNoInteractions(resolver,resources);
    }

    @Test
    void shouldCheckStatusWithoutLoadingContentOrCountingInbox() {
        when(mapper.selectOne(any())).thenReturn(UserNotification.builder().id(7L).build(), null);
        assertThat(service.getStatus(new NotificationStatusParamsBO(1L,2L)).isHasUnread()).isTrue();
        assertThat(service.getStatus(new NotificationStatusParamsBO(1L,2L)).isHasUnread()).isFalse();
        verify(mapper,never()).selectCount(any());
        verifyNoInteractions(resolver,resources);
    }

    @Test
    void shouldSignPageImagesOnceAndChooseFirstReadableImageInCurrentOrganization() {
        when(mapper.selectList(any())).thenReturn(List.of(row(1L),row(2L),row(3L)));
        when(mapper.selectCount(any())).thenReturn(3L);
        when(resolver.resolve(any())).thenReturn(Map.of(
                1L,source(List.of(10L,11L)),2L,source(List.of(10L,12L)),3L,source(List.of(13L))));
        when(resources.getResourceReadUrls(anyList())).thenReturn(Map.of(
                10L,new ResourceReadUrlGetResultBO("https://wrong.example/img",99L,"wrong"),
                11L,new ResourceReadUrlGetResultBO("https://cdn.example/cover?sign=ok",2L,"cover"),
                12L,new ResourceReadUrlGetResultBO("http://localhost/local?key=thumb",2L,"thumb")));
        var page=service.list(new NotificationListParamsBO(1L,2L,null,null));
        assertThat(page.getList()).extracting(NotificationResultBO::getThumbnailUrl).containsExactly(
                "https://cdn.example/cover?sign=ok&eo-img.resize=w/144/h/144",
                "http://localhost/local?key=thumb&eo-img.resize=w/144/h/144",null);
        verify(resources).getResourceReadUrls(anyList());
    }

    @Test
    void shouldNotMaskImageSigningFailureAsEmptyThumbnail() {
        when(mapper.selectList(any())).thenReturn(List.of(row(1L)));
        when(mapper.selectCount(any())).thenReturn(1L);
        when(resolver.resolve(any())).thenReturn(Map.of(1L,source(List.of(10L))));
        when(resources.getResourceReadUrls(anyList())).thenThrow(new IllegalStateException("signer unavailable"));
        assertThatThrownBy(() -> service.list(new NotificationListParamsBO(1L,2L,null,null)))
                .isInstanceOf(IllegalStateException.class).hasMessage("signer unavailable");
    }

    @Test
    void shouldNotEnqueueWhenNothingWasNotified() {
        service.createBatch(new NotificationBatchCreateParamsBO(1L, List.of()));
        verifyNoInteractions(writer);
    }

    @Test
    void shouldRejectMissingNotificationIdsWithoutEnqueue() {
        InboxNotificationServiceImpl spyService = spy(service);
        doReturn(true).when(spyService).saveBatch(anyCollection());
        assertThatThrownBy(() -> spyService.createBatch(new NotificationBatchCreateParamsBO(1L, List.of(created()))))
                .isInstanceOf(IllegalStateException.class).hasMessage("站内通知未生成ID");
        verifyNoInteractions(writer);
    }

    @Test
    void shouldEnqueueNotificationsAfterIdsAreGenerated() {
        InboxNotificationServiceImpl spyService = spy(service);
        doAnswer(invocation -> {
            List<UserNotification> rows = invocation.getArgument(0);
            long id = 11;
            for (UserNotification row : rows) {
                row.setId(id++);
            }
            return true;
        }).when(spyService).saveBatch(anyCollection());
        spyService.createBatch(new NotificationBatchCreateParamsBO(8L, List.of(created(), created())));
        verify(writer).enqueue(eq(8L), argThat(rows -> rows.stream().map(UserNotification::getId).toList().equals(List.of(11L, 12L))));
    }

    private NotificationCreateBO created() {
        return NotificationCreateBO.builder().recipientUserId(4L).kind(InboxNotificationKind.TASK_READY)
                .sourceId(202L).triggerId(9L).createdAt(LocalDateTime.now(clock)).build();
    }

    private UserNotification row(Long id) {
        return UserNotification.builder().id(id).kind(InboxNotificationKind.TASK_READY).sourceId(7L)
                .createdAt(LocalDateTime.now(clock)).build();
    }
    private NotificationResolvedSourceBO source(List<Long> images) {
        return new NotificationResolvedSourceBO(new NotificationSourceResultBO(1L,"企划",7L,"项目",null),images);
    }
}
