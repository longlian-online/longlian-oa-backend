package online.longlian.app.service.app.impl.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import online.longlian.app.common.util.MailUtil;
import online.longlian.app.service.app.UserService;
import online.longlian.app.pojo.bo.app.NotificationResolvedSourceBO;
import online.longlian.app.pojo.bo.app.NotificationSourceResolveParamsBO;
import online.longlian.app.pojo.bo.app.NotificationSourceResultBO;
import online.longlian.app.pojo.bo.app.NotificationTaskSlotBO;
import online.longlian.app.pojo.entity.OutboxMessage;
import online.longlian.app.pojo.bo.app.UserEmailNotificationRecipientResultBO;
import online.longlian.app.service.outbox.OutboxPermanentException;
import online.longlian.app.service.outbox.OutboxSkipException;
import online.longlian.common.enumeration.InboxNotificationKind;
import online.longlian.common.enumeration.OutboxMessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InboxEmailOutboxHandlerTest {
    private static final LocalDateTime EXECUTE_TIME = LocalDateTime.of(2026, 10, 23, 9, 0);
    private static final String DISPLAY_NAME = "龙联<OA>";
    private static final String FROM = "mailer@example.com";
    private static final String WEB = "https://oa.example/";

    @Mock NotificationSourceResolver resolver;
    @Mock UserService userService;
    @Mock MailUtil mailUtil;
    InboxEmailOutboxHandler handler;
    final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        handler = handler(new DefaultResourceLoader(), DISPLAY_NAME);
    }

    @Test
    void shouldSendParallelSlotCopyWithEscapedTitleAndTrimmedRecipient() throws Exception {
        String message = "「企划<A> · 项目」的前置步骤已完成，你接取的「翻译 · 2/2」可以开始了。";
        when(resolver.resolve(any())).thenReturn(Map.of(9L, source("企划<A>", "项目",
                new NotificationTaskSlotBO("翻译", 2, 2, null))));
        when(userService.getEmailNotificationRecipient(4L)).thenReturn(
                new UserEmailNotificationRecipientResultBO("  user@example.com  ", true));

        handler.deliver(message(InboxNotificationKind.TASK_READY, 202L), EXECUTE_TIME);

        ArgumentCaptor<NotificationSourceResolveParamsBO> params = ArgumentCaptor.forClass(NotificationSourceResolveParamsBO.class);
        verify(resolver).resolve(params.capture());
        assertThat(params.getValue().getOrgId()).isEqualTo(1L);
        assertThat(params.getValue().getSources()).singleElement().satisfies(ref -> {
            assertThat(ref.getNotificationId()).isEqualTo(9L);
            assertThat(ref.getKind()).isEqualTo(InboxNotificationKind.TASK_READY);
            assertThat(ref.getSourceId()).isEqualTo(202L);
        });
        MailUtil.SendParam sent = sent();
        assertThat(sent.sender()).isEqualTo(FROM);
        assertThat(sent.receiver()).isEqualTo("user@example.com");
        assertThat(sent.receiverName()).isEqualTo(DISPLAY_NAME);
        assertThat(sent.title()).isEqualTo(DISPLAY_NAME + " 可以开始");
        assertThat(sent.content()).contains("<title>" + HtmlUtils.htmlEscape(DISPLAY_NAME) + "</title>");
        assertThat(sent.content()).doesNotContain("<title>" + DISPLAY_NAME + "</title>");
        assertThat(sent.content()).contains("<p>" + HtmlUtils.htmlEscape(message) + "</p>");
        assertThat(sent.content()).contains("href=\"" + HtmlUtils.htmlEscape(link(InboxNotificationKind.TASK_READY, 202L)) + "\"");
        assertThat(sent.content()).doesNotContain("企划<A>");
        assertThat(InboxEmailCopy.message(InboxNotificationKind.TASK_READY, new NotificationSourceResultBO(
                1L, "企划<A>", 7L, "项目", new NotificationTaskSlotBO("翻译", 2, 2, null)))).isEqualTo(message);
    }

    @Test
    void shouldSendCrossStageAndCompletionSentences() throws Exception {
        String ready = "「企划 · 项目」的前置步骤已完成，你接取的「翻译（第 2 步）」可以开始了。";
        when(resolver.resolve(any())).thenReturn(Map.of(9L, source("企划", "项目",
                new NotificationTaskSlotBO("翻译", 1, 1, 2))));
        when(userService.getEmailNotificationRecipient(4L)).thenReturn(recipient());
        handler.deliver(message(InboxNotificationKind.TASK_READY, 202L), EXECUTE_TIME);
        assertThat(sent().content()).contains(HtmlUtils.htmlEscape(ready));
        assertThat(sent().title()).isEqualTo(DISPLAY_NAME + " 可以开始");
        assertThat(InboxEmailCopy.slotLabel(new NotificationTaskSlotBO("翻译", 1, 1, 0))).isEqualTo("翻译");

        when(resolver.resolve(any())).thenReturn(Map.of(9L, source("企划", "项目", null)));
        handler.deliver(message(InboxNotificationKind.ITEM_COMPLETED, 100L), EXECUTE_TIME);
        String completed = "「企划 · 项目」的全部任务已完成。";
        assertThat(sent().content()).contains(HtmlUtils.htmlEscape(completed));
        assertThat(sent().content()).contains("href=\"" + HtmlUtils.htmlEscape(link(InboxNotificationKind.ITEM_COMPLETED, 100L)) + "\"");
        assertThat(sent().content()).doesNotContain("taskInstanceId");
        assertThat(sent().title()).isEqualTo(DISPLAY_NAME + " 项目完成");
        assertThat(InboxEmailCopy.message(InboxNotificationKind.ITEM_COMPLETED,
                new NotificationSourceResultBO(1L, "企划", 7L, "项目", null))).isEqualTo(completed);
    }

    @Test
    void shouldSendAssignmentCopyToAssignee() throws Exception {
        String assigned = "「企划 · 项目」的「翻译 · 2/2」已分配给你，无需手动领取。";
        when(resolver.resolve(any())).thenReturn(Map.of(9L, source("企划", "项目",
                new NotificationTaskSlotBO("翻译", 2, 2, null))));
        when(userService.getEmailNotificationRecipient(4L)).thenReturn(recipient());

        handler.deliver(message(InboxNotificationKind.TASK_ASSIGNED, 202L), EXECUTE_TIME);

        assertThat(sent().content()).contains(HtmlUtils.htmlEscape(assigned));
        assertThat(sent().content()).contains("href=\"" + HtmlUtils.htmlEscape(link(InboxNotificationKind.TASK_ASSIGNED, 202L)) + "\"");
        assertThat(sent().title()).isEqualTo(DISPLAY_NAME + " 任务分配");
        assertThat(InboxEmailCopy.message(InboxNotificationKind.TASK_ASSIGNED, new NotificationSourceResultBO(
                1L, "企划", 7L, "项目", new NotificationTaskSlotBO("翻译", 2, 2, null)))).isEqualTo(assigned);
    }




    @Test
    void shouldNotRescanPlaceholdersInsertedByDisplayNameOrMessage() throws Exception {
        InboxEmailOutboxHandler named = handler(new DefaultResourceLoader(), "名${displayName}");
        when(resolver.resolve(any())).thenReturn(Map.of(9L, source("企划${message}", "项目${displayName}", null)));
        when(userService.getEmailNotificationRecipient(4L)).thenReturn(recipient());

        named.deliver(message(InboxNotificationKind.ITEM_COMPLETED, 100L), EXECUTE_TIME);

        MailUtil.SendParam sent = sent();
        assertThat(sent.content()).contains("<title>名${displayName}</title>");
        assertThat(sent.content()).contains("企划${message}");
        assertThat(sent.content()).contains("项目${displayName}");
        assertThat(sent.content()).doesNotContain("名名");
    }

    @Test
    void shouldSkipBlankEmailMissingUserAndUnavailableSourceWithoutSending() {
        when(userService.getEmailNotificationRecipient(4L)).thenReturn(
                new UserEmailNotificationRecipientResultBO(null, true),
                new UserEmailNotificationRecipientResultBO("", true),
                new UserEmailNotificationRecipientResultBO("  ", true));
        assertThatThrownBy(() -> handler.deliver(message(InboxNotificationKind.TASK_READY, 202L), EXECUTE_TIME))
                .isInstanceOf(OutboxSkipException.class).hasMessage("收件人未绑定邮箱");
        assertThatThrownBy(() -> handler.deliver(message(InboxNotificationKind.TASK_READY, 202L), EXECUTE_TIME))
                .isInstanceOf(OutboxSkipException.class).hasMessage("收件人未绑定邮箱");
        assertThatThrownBy(() -> handler.deliver(message(InboxNotificationKind.TASK_READY, 202L), EXECUTE_TIME))
                .isInstanceOf(OutboxSkipException.class).hasMessage("收件人未绑定邮箱");

        when(userService.getEmailNotificationRecipient(4L)).thenReturn(null);
        assertThatThrownBy(() -> handler.deliver(message(InboxNotificationKind.TASK_READY, 202L), EXECUTE_TIME))
                .isInstanceOf(OutboxSkipException.class).hasMessage("收件人不存在");

        when(userService.getEmailNotificationRecipient(4L)).thenReturn(recipient());
        when(resolver.resolve(any())).thenReturn(Map.of(9L, new NotificationResolvedSourceBO(null, List.of())));
        assertThatThrownBy(() -> handler.deliver(message(InboxNotificationKind.TASK_READY, 202L), EXECUTE_TIME))
                .isInstanceOf(OutboxSkipException.class).hasMessage("来源不可用");
        verifyNoInteractions(mailUtil);
    }

    @ParameterizedTest
    @EnumSource(InboxNotificationKind.class)
    void shouldSkipBothNotificationKindsWhenEmailNotificationsAreDisabled(InboxNotificationKind kind) {
        when(userService.getEmailNotificationRecipient(4L)).thenReturn(
                new UserEmailNotificationRecipientResultBO("user@example.com", false));

        assertThatThrownBy(() -> handler.deliver(message(kind, 202L), EXECUTE_TIME))
                .isInstanceOf(OutboxSkipException.class);
        verifyNoInteractions(resolver, mailUtil);
    }

    @Test
    void shouldSkipTaskReadyWhenSlotIsMissing() {
        when(resolver.resolve(any())).thenReturn(Map.of(9L, source("企划", "项目", null)));
        when(userService.getEmailNotificationRecipient(4L)).thenReturn(recipient());
        assertThatThrownBy(() -> handler.deliver(message(InboxNotificationKind.TASK_READY, 202L), EXECUTE_TIME))
                .isInstanceOf(OutboxSkipException.class).hasMessage("来源不可用");
        verifyNoInteractions(mailUtil);
    }

    @Test
    void shouldRejectUnreadablePayload() {
        OutboxMessage message = OutboxMessage.builder().payload("{").build();
        assertThatThrownBy(() -> handler.deliver(message, EXECUTE_TIME))
                .isInstanceOf(OutboxPermanentException.class).hasMessage("payload 无法解析");
        message.setPayload("{\"notificationId\":9}");
        assertThatThrownBy(() -> handler.deliver(message, EXECUTE_TIME))
                .isInstanceOf(OutboxPermanentException.class).hasMessage("payload 无法解析");
        verifyNoInteractions(resolver, userService, mailUtil);
    }

    @Test
    void shouldRejectEachMissingPayloadField() {
        List<String> payloads = List.of(
                "{\"orgId\":1,\"recipientUserId\":4,\"kind\":\"TASK_READY\",\"sourceId\":202}",
                "{\"notificationId\":9,\"recipientUserId\":4,\"kind\":\"TASK_READY\",\"sourceId\":202}",
                "{\"notificationId\":9,\"orgId\":1,\"kind\":\"TASK_READY\",\"sourceId\":202}",
                "{\"notificationId\":9,\"orgId\":1,\"recipientUserId\":4,\"sourceId\":202}",
                "{\"notificationId\":9,\"orgId\":1,\"recipientUserId\":4,\"kind\":\"TASK_READY\"}");
        for (String payload : payloads) {
            assertThatThrownBy(() -> handler.deliver(OutboxMessage.builder().payload(payload).build(), EXECUTE_TIME))
                    .isInstanceOf(OutboxPermanentException.class).hasMessage("payload 无法解析");
        }
        verifyNoInteractions(mailUtil);
    }

    @Test
    void shouldSkipWhenResolvedSourceIsAbsent() {
        when(resolver.resolve(any())).thenReturn(Map.of());
        when(userService.getEmailNotificationRecipient(4L)).thenReturn(recipient());
        assertThatThrownBy(() -> handler.deliver(message(InboxNotificationKind.TASK_READY, 202L), EXECUTE_TIME))
                .isInstanceOf(OutboxSkipException.class).hasMessage("来源不可用");
        verifyNoInteractions(mailUtil);
    }

    @Test
    void shouldFailConstructionWhenTemplateIsMissing() {
        assertThatThrownBy(() -> handler(new DefaultResourceLoader() {
            @Override
            public org.springframework.core.io.Resource getResource(String location) {
                return new ClassPathResource("templates/missing-inbox-notification.html");
            }
        }, DISPLAY_NAME)).isInstanceOf(IllegalStateException.class).hasMessage("站内通知邮件模板缺失");
    }

    @Test
    void shouldFailConstructionWhenTemplateCannotBeRead() {
        assertThatThrownBy(() -> handler(new DefaultResourceLoader() {
            @Override
            public org.springframework.core.io.Resource getResource(String location) {
                return new ClassPathResource("templates/inbox-notification.html") {
                    @Override
                    public java.io.InputStream getInputStream() throws java.io.IOException {
                        throw new java.io.IOException("boom");
                    }
                };
            }
        }, DISPLAY_NAME)).isInstanceOf(IllegalStateException.class)
                .hasMessage("站内通知邮件模板缺失")
                .hasCauseInstanceOf(java.io.IOException.class);
    }
    @Test
    void shouldRejectWebUrlThatIsNotAnOrigin() {
        assertThatThrownBy(() -> handler(new DefaultResourceLoader(), DISPLAY_NAME, "https://user:secret@oa.example/app?x=1"))
                .isInstanceOf(IllegalStateException.class).hasMessage("longlian.web-url 无效");
    }

    private InboxEmailOutboxHandler handler(org.springframework.core.io.ResourceLoader resources, String displayName) {
        return handler(resources, displayName, WEB);
    }

    private InboxEmailOutboxHandler handler(org.springframework.core.io.ResourceLoader resources, String displayName, String webUrl) {
        return new InboxEmailOutboxHandler(resolver, userService, mailUtil, objectMapper, resources, FROM, displayName, webUrl);
    }

    private OutboxMessage message(InboxNotificationKind kind, long sourceId) throws Exception {
        return OutboxMessage.builder().type(OutboxMessageType.INBOX_EMAIL)
                .payload(objectMapper.writeValueAsString(new InboxEmailPayload(9L, 1L, 4L, kind, sourceId))).build();
    }

    private static UserEmailNotificationRecipientResultBO recipient() {
        return new UserEmailNotificationRecipientResultBO("user@example.com", true);
    }

    private static NotificationResolvedSourceBO source(String projectTitle, String itemTitle, NotificationTaskSlotBO slot) {
        return new NotificationResolvedSourceBO(new NotificationSourceResultBO(1L, projectTitle, 7L, itemTitle, slot),
                List.of());
    }

    private static String link(InboxNotificationKind kind, long sourceId) {
        return InboxEmailLink.href("https://oa.example", kind, 1L,
                new NotificationSourceResultBO(1L, "企划", 7L, "项目", null), sourceId);
    }
    private MailUtil.SendParam sent() {
        ArgumentCaptor<MailUtil.SendParam> captor = ArgumentCaptor.forClass(MailUtil.SendParam.class);
        verify(mailUtil, org.mockito.Mockito.atLeastOnce()).send(captor.capture());
        return captor.getValue();
    }
}
