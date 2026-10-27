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
        URI uri = parseWebUrl(webUrl);
        if (!isRootHttpOrigin(uri)) {
            throw invalidWebUrl(null);
        }
        return uri.getScheme() + "://" + uri.getRawAuthority();
    }

    private static URI parseWebUrl(String webUrl) {
        if (webUrl == null || webUrl.isBlank() || webUrl.contains("${")) {
            throw invalidWebUrl(null);
        }
        try {
            return URI.create(webUrl.trim());
        } catch (IllegalArgumentException exception) {
            throw invalidWebUrl(exception);
        }
    }

    private static boolean isRootHttpOrigin(URI uri) {
        String scheme = uri.getScheme();
        return ("http".equals(scheme) || "https".equals(scheme)) && hasBareAuthority(uri) && isRootPath(uri.getPath());
    }

    private static boolean hasBareAuthority(URI uri) {
        return uri.getHost() != null && uri.getUserInfo() == null && uri.getRawQuery() == null
                && uri.getRawFragment() == null && uri.getRawAuthority() != null;
    }

    private static boolean isRootPath(String path) {
        return path == null || path.isEmpty() || "/".equals(path);
    }

    private static IllegalStateException invalidWebUrl(Exception cause) {
        return new IllegalStateException("longlian.web-url 无效", cause);
    }

    static String href(String origin, InboxNotificationKind kind, long orgId,
                       NotificationSourceResultBO source, long sourceId) {
        if (kind == InboxNotificationKind.JOIN_APPLICATION || kind == InboxNotificationKind.DELETION_REQUEST) {
            String idName = kind == InboxNotificationKind.JOIN_APPLICATION ? "applicationId" : "requestId";
            return UriComponentsBuilder.fromUriString(origin + "/go")
                    .queryParam("type", kind.name())
                    .queryParam("orgId", orgId)
                    .queryParam(idName, sourceId)
                    .encode().build().toUriString();
        }
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
