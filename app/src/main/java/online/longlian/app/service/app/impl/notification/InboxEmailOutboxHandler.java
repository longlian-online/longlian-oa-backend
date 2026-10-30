package online.longlian.app.service.app.impl.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import online.longlian.app.common.util.MailUtil;
import online.longlian.app.pojo.bo.app.UserEmailNotificationRecipientResultBO;
import online.longlian.app.pojo.bo.app.NotificationSourceRefBO;
import online.longlian.app.pojo.bo.app.NotificationSourceResolveParamsBO;
import online.longlian.app.pojo.bo.app.NotificationSourceResultBO;
import online.longlian.app.pojo.entity.OutboxMessage;
import online.longlian.app.service.app.UserService;
import online.longlian.app.service.outbox.OutboxHandler;
import online.longlian.app.service.outbox.OutboxPermanentException;
import online.longlian.app.service.outbox.OutboxSkipException;
import online.longlian.common.enumeration.InboxNotificationKind;
import online.longlian.common.enumeration.OutboxMessageType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class InboxEmailOutboxHandler implements OutboxHandler {
    private static final String TEMPLATE = "classpath:templates/inbox-notification.html";

    private final NotificationSourceResolver sourceResolver;
    private final UserService userService;
    private final MailUtil mailUtil;
    private final ObjectMapper objectMapper;
    private final String fromEmail;
    private final String displayName;
    private final String webOrigin;
    private final String htmlTemplate;

    public InboxEmailOutboxHandler(NotificationSourceResolver sourceResolver, UserService userService, MailUtil mailUtil,
                                   ObjectMapper objectMapper, ResourceLoader resourceLoader,
                                   @Value("${spring.mail.username}") String fromEmail,
                                   @Value("${longlian.display-name}") String displayName,
                                   @Value("${longlian.web-url}") String webUrl) {
        this.sourceResolver = sourceResolver;
        this.userService = userService;
        this.mailUtil = mailUtil;
        this.objectMapper = objectMapper;
        this.fromEmail = fromEmail;
        this.displayName = displayName;
        this.webOrigin = InboxEmailLink.origin(webUrl);
        String raw = load(resourceLoader);
        int linkAt = raw.indexOf("${link}");
        if (linkAt < 0 || raw.indexOf("${link}", linkAt + "${link}".length()) >= 0) {
            throw new IllegalStateException("站内通知邮件模板缺失");
        }
        String withLink = raw.substring(0, linkAt) + InboxEmailLink.PLACEHOLDER
                + raw.substring(linkAt + "${link}".length());
        this.htmlTemplate = withLink.replace("${displayName}", HtmlUtils.htmlEscape(displayName));
    }

    @Override
    public OutboxMessageType type() {
        return OutboxMessageType.INBOX_EMAIL;
    }

    @Override
    public void deliver(OutboxMessage message, LocalDateTime executeTime) {
        InboxEmailPayload payload = payload(message.getPayload());
        UserEmailNotificationRecipientResultBO recipient = userService.getEmailNotificationRecipient(payload.recipientUserId());
        if (recipient == null) {
            throw new OutboxSkipException("收件人不存在");
        }
        if (!recipient.isEmailNotificationsEnabled()) {
            throw new OutboxSkipException("收件人已关闭邮件通知");
        }
        if (recipient.rejectsKind(payload.kind())) {
            throw new OutboxSkipException("收件人已关闭该类型通知");
        }
        String email = recipient.getEmail();
        if (email == null || email.trim().isEmpty()) {
            throw new OutboxSkipException("收件人未绑定邮箱");
        }
        NotificationSourceResultBO source = source(payload);
        String body = InboxEmailCopy.message(payload.kind(), source);
        String href = InboxEmailLink.href(webOrigin, payload.kind(), payload.orgId(), source, payload.sourceId());
        String html = htmlTemplate.replace(InboxEmailLink.PLACEHOLDER, HtmlUtils.htmlEscape(href))
                .replace("${message}", HtmlUtils.htmlEscape(body));
        mailUtil.send(new MailUtil.SendParam(fromEmail, email.trim(), displayName,
                displayName + " " + payload.kind().getDesc(), html));
    }

    private NotificationSourceResultBO source(InboxEmailPayload payload) {
        var resolved = sourceResolver.resolve(new NotificationSourceResolveParamsBO(payload.orgId(),
                List.of(new NotificationSourceRefBO(payload.notificationId(), payload.kind(), payload.sourceId()))));
        var match = resolved.get(payload.notificationId());
        NotificationSourceResultBO source = match == null ? null : match.getSource();
        if (source == null || ((payload.kind() == InboxNotificationKind.TASK_READY
                || payload.kind() == InboxNotificationKind.TASK_ASSIGNED) && source.getTaskSlot() == null)) {
            throw new OutboxSkipException("来源不可用");
        }
        return source;
    }

    private InboxEmailPayload payload(String json) {
        InboxEmailPayload payload;
        try {
            payload = objectMapper.readValue(json, InboxEmailPayload.class);
        } catch (JsonProcessingException exception) {
            throw new OutboxPermanentException("payload 无法解析");
        }
        if (payload.notificationId() == null || payload.orgId() == null || payload.recipientUserId() == null
                || payload.kind() == null || payload.sourceId() == null) {
            throw new OutboxPermanentException("payload 无法解析");
        }
        return payload;
    }

    private static String load(ResourceLoader resourceLoader) {
        Resource resource = resourceLoader.getResource(TEMPLATE);
        if (!resource.exists()) {
            throw new IllegalStateException("站内通知邮件模板缺失");
        }
        try {
            return new String(FileCopyUtils.copyToByteArray(resource.getInputStream()), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("站内通知邮件模板缺失", exception);
        }
    }
}
