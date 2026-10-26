package online.longlian.app.service.app.impl.notification;

import online.longlian.app.pojo.bo.app.NotificationSourceResultBO;
import online.longlian.common.enumeration.InboxNotificationKind;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

final class InboxEmailLink {
    static final String PLACEHOLDER = "\u0000link\u0000";

    private InboxEmailLink() {
    }

    static String origin(String webUrl) {
        if (webUrl == null || webUrl.isBlank() || webUrl.contains("${")) {
            throw new IllegalStateException("longlian.web-url 无效");
        }
        URI uri;
        try {
            uri = URI.create(webUrl.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("longlian.web-url 无效", exception);
        }
        String scheme = uri.getScheme();
        boolean rooted = uri.getPath() == null || uri.getPath().isEmpty() || "/".equals(uri.getPath());
        if ((!"http".equals(scheme) && !"https".equals(scheme)) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null
                || !rooted || uri.getRawAuthority() == null) {
            throw new IllegalStateException("longlian.web-url 无效");
        }
        return scheme + "://" + uri.getRawAuthority();
    }

    static String href(String origin, InboxNotificationKind kind, long orgId,
                       NotificationSourceResultBO source, long sourceId) {
        if (source.getProjectId() == null || source.getItemId() == null) {
            throw new IllegalStateException("来源不可用");
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(origin + "/go")
                .queryParam("type", kind.name())
                .queryParam("orgId", orgId)
                .queryParam("projectId", source.getProjectId())
                .queryParam("itemId", source.getItemId());
        if (kind == InboxNotificationKind.TASK_READY || kind == InboxNotificationKind.TASK_ASSIGNED) {
            builder.queryParam("taskInstanceId", sourceId);
        }
        return builder.encode().build().toUriString();
    }
}
