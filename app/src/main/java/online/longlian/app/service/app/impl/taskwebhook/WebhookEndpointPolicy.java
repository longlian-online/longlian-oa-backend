package online.longlian.app.service.app.impl.taskwebhook;

import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import org.springframework.stereotype.Component;

/** 地址和密钥成对出现。编辑时留空密钥表示沿用原来的，清空地址则同时丢掉密钥。 */
@Component
@RequiredArgsConstructor
public class WebhookEndpointPolicy {
    public record Endpoint(String url, String secret) {
    }

    private final TaskWebhookAddressGuard addressGuard;

    public Endpoint resolve(String url, String secret, String existingSecret) {
        String normalizedUrl = blankToNull(url);
        String normalizedSecret = blankToNull(secret);
        if (normalizedUrl == null) {
            if (normalizedSecret != null) {
                throw invalid("未填写 webhook 地址");
            }
            return new Endpoint(null, null);
        }
        if (normalizedUrl.length() > 500) {
            throw invalid("webhook 地址不能超过 500 个字符");
        }
        try {
            addressGuard.require(normalizedUrl);
        } catch (TaskWebhookTransientException exception) {
            throw invalid(exception.getMessage());
        } catch (IllegalArgumentException exception) {
            throw invalid(exception.getMessage());
        }
        String nextSecret = normalizedSecret != null ? normalizedSecret : blankToNull(existingSecret);
        if (nextSecret == null) {
            throw invalid("请填写 webhook 签名密钥");
        }
        if (nextSecret.length() < 16 || nextSecret.length() > 128) {
            throw invalid("webhook 签名密钥长度必须为 16 到 128 个字符");
        }
        return new Endpoint(normalizedUrl, nextSecret);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static AppException invalid(String message) {
        return new AppException(ResultCode.PARAM_ERROR, message);
    }
}
