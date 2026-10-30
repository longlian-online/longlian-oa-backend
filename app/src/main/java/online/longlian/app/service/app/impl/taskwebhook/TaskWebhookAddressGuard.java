package online.longlian.app.service.app.impl.taskwebhook;

import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;

/**
 * 只允许公网 HTTPS 443。保存和投递共用同一套判断，避免配置时放过、发送时才发现打到内网。
 */
@Component
public class TaskWebhookAddressGuard {
    private static final Set<String> BLOCKED_NAMES = Set.of(
            "localhost", "localhost.localdomain", "metadata.google.internal", "metadata.internal");

    public URI require(String url) {
        URI uri = parse(url);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getRawUserInfo() != null
                || uri.getHost() == null || uri.getHost().isBlank() || uri.getRawFragment() != null) {
            throw rejected("webhook 只接受不带用户信息的 HTTPS 地址");
        }
        if (uri.getPort() != -1 && uri.getPort() != 443) {
            throw rejected("webhook 只接受 443 端口");
        }
        String host = stripDot(uri.getHost());
        if (BLOCKED_NAMES.contains(host.toLowerCase(Locale.ROOT))) {
            throw rejected("webhook 地址不能指向内网");
        }
        for (InetAddress address : resolve(host)) {
            if (isBlocked(address)) {
                throw rejected("webhook 地址不能指向内网");
            }
        }
        return uri;
    }

    private static URI parse(String url) {
        try {
            return URI.create(url);
        } catch (IllegalArgumentException exception) {
            throw rejected("webhook 地址无效");
        }
    }

    private static InetAddress[] resolve(String host) {
        try {
            return InetAddress.getAllByName(host);
        } catch (UnknownHostException exception) {
            throw new TaskWebhookTransientException("webhook 地址暂时无法解析");
        }
    }

    private static boolean isBlocked(InetAddress address) {
        InetAddress unwrapped = unwrapMappedIpv4(address);
        if (unwrapped != address) {
            return isBlocked(unwrapped);
        }
        if (isLocalOrMulticast(address)) {
            return true;
        }
        return isReservedRange(address.getAddress());
    }

    private static boolean isLocalOrMulticast(InetAddress address) {
        return address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress();
    }

    private static boolean isReservedRange(byte[] raw) {
        if (raw.length == 4) {
            int first = raw[0] & 0xff;
            int second = raw[1] & 0xff;
            return first == 0 || first == 255 || (first == 100 && second >= 64 && second <= 127);
        }
        return raw.length == 16 && (raw[0] & 0xfe) == 0xfc;
    }

    private static InetAddress unwrapMappedIpv4(InetAddress address) {
        byte[] raw = address.getAddress();
        if (raw.length != 16 || !isMappedIpv4(raw)) {
            return address;
        }
        try {
            return InetAddress.getByAddress(new byte[]{raw[12], raw[13], raw[14], raw[15]});
        } catch (UnknownHostException exception) { // skipcq: TCV-001
            return address; // skipcq: TCV-001
        }
    }

    private static boolean isMappedIpv4(byte[] raw) {
        for (int index = 0; index < 10; index++) {
            if (raw[index] != 0) {
                return false;
            }
        }
        return (raw[10] & 0xff) == 0xff && (raw[11] & 0xff) == 0xff;
    }

    private static String stripDot(String host) {
        return host.endsWith(".") ? host.substring(0, host.length() - 1) : host;
    }

    private static IllegalArgumentException rejected(String message) {
        return new IllegalArgumentException(message);
    }
}
