package online.longlian.app.api.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import online.longlian.app.common.util.MailUtil;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.app.NotificationResolvedSourceBO;
import online.longlian.app.pojo.bo.app.NotificationSourceResultBO;
import online.longlian.app.pojo.bo.app.NotificationTaskSlotBO;
import online.longlian.app.pojo.bo.app.UserEmailNotificationRecipientResultBO;
import online.longlian.app.pojo.entity.OutboxMessage;
import online.longlian.app.service.app.UserService;
import online.longlian.app.service.app.impl.notification.InboxEmailOutboxHandler;
import online.longlian.app.service.app.impl.notification.InboxEmailPayload;
import online.longlian.app.service.app.impl.notification.NotificationSourceResolver;
import online.longlian.app.service.app.impl.taskinstance.TaskInstanceServiceImpl;
import online.longlian.common.enumeration.InboxNotificationKind;
import online.longlian.common.enumeration.OutboxMessageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * API 覆盖率报告只保留 -Papi-test 的执行数据，这些分支在单元测试里已经走到，这里再走一次。
 */
@ExtendWith(MockitoExtension.class)
class AssignNotificationCoverageTest {

    @Mock
    private NotificationSourceResolver resolver;
    @Mock
    private UserService userService;
    @Mock
    private MailUtil mailUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldRejectBlankUnresolvedAndIllegalWebUrls() {
        assertThatThrownBy(() -> handler(" ")).isInstanceOf(IllegalStateException.class).hasMessage("longlian.web-url 无效");
        assertThatThrownBy(() -> handler("${web-url}")).isInstanceOf(IllegalStateException.class).hasMessage("longlian.web-url 无效");
        assertThatThrownBy(() -> handler("http://[")).isInstanceOf(IllegalStateException.class).hasMessage("longlian.web-url 无效");
        assertThatThrownBy(() -> handler("https://user:secret@oa.example/app?x=1"))
                .isInstanceOf(IllegalStateException.class).hasMessage("longlian.web-url 无效");
    }

    @Test
    void shouldRejectMissingTemplateAndUnavailableTaskSource() throws Exception {
        assertThatThrownBy(() -> new InboxEmailOutboxHandler(resolver, userService, mailUtil, objectMapper,
                new DefaultResourceLoader() {
                    @Override
                    public org.springframework.core.io.Resource getResource(String location) {
                        return new ClassPathResource("templates/missing-inbox-notification.html");
                    }
                }, "mailer@example.com", "龙联", "https://oa.example/"))
                .isInstanceOf(IllegalStateException.class).hasMessage("站内通知邮件模板缺失");

        InboxEmailOutboxHandler ready = handler("https://oa.example/");
        when(userService.getEmailNotificationRecipient(4L))
                .thenReturn(new UserEmailNotificationRecipientResultBO("user@example.com", true));
        when(resolver.resolve(any())).thenReturn(Map.of(9L, new NotificationResolvedSourceBO(
                new NotificationSourceResultBO(null, "企划", null, "项目", new NotificationTaskSlotBO("翻译", 1, 1, null)),
                List.of())));
        OutboxMessage message = OutboxMessage.builder().type(OutboxMessageType.INBOX_EMAIL)
                .payload(objectMapper.writeValueAsString(
                        new InboxEmailPayload(9L, 1L, 4L, InboxNotificationKind.TASK_ASSIGNED, 202L)))
                .build();
        assertThatThrownBy(() -> ready.deliver(message, LocalDateTime.of(2026, 10, 26, 9, 0)))
                .isInstanceOf(IllegalStateException.class).hasMessage("来源不可用");
    }

    @Test
    void shouldReturnNoAssignableMembersWhenNoneAreEnabled() {
        OrganizationMemberMapper members = mock(OrganizationMemberMapper.class);
        UserMapper users = mock(UserMapper.class);
        when(members.selectList(any())).thenReturn(List.of());
        TaskInstanceServiceImpl service = new TaskInstanceServiceImpl(
                null, members, users, null, null, null, null, null, null, null, null, null, null, null, null,
                Clock.systemUTC());

        assertThat(service.listAssignableMembers(1L)).isEmpty();
        verifyNoInteractions(users);
    }

    private InboxEmailOutboxHandler handler(String webUrl) {
        return new InboxEmailOutboxHandler(resolver, userService, mailUtil, objectMapper, new DefaultResourceLoader(),
                "mailer@example.com", "龙联", webUrl);
    }
}
