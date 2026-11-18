package online.longlian.app.service.captcha;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.properties.CaptchaProperties;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.bo.common.CaptchaConfigResultBO;
import online.longlian.app.pojo.bo.common.CaptchaVerifyParamsBO;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 通用 FCaptcha 验票入口：决定公开配置，并把票据交给人机验证服务复核。
 * 决不把验票地址或密钥暴露给浏览器；网络、状态、解析失败一律拒绝受保护操作。
 */
@Slf4j
@Service
public class CaptchaService {
    private final CaptchaProperties properties;
    private final HttpClient client;

    public CaptchaService(CaptchaProperties properties) {
        this.properties = properties;
        this.client = properties.isEnabled() ? HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build() : null;
    }

    public CaptchaConfigResultBO getConfig() {
        if (!properties.isEnabled()) {
            return CaptchaConfigResultBO.builder().enabled(false).build();
        }
        return CaptchaConfigResultBO.builder()
                .enabled(true)
                .serverUrl(properties.getServerUrl().toString())
                .siteKey(properties.getSiteKey())
                .build();
    }

    public void verify(CaptchaVerifyParamsBO params) {
        if (!properties.isEnabled()) return;
        JSONObject verdict = requestVerdict(params);
        if (verdict == null || !(verdict.get("success") instanceof Boolean)) {
            throw new AppException(ResultCode.CAPTCHA_UNAVAILABLE);
        }
        if (!Boolean.TRUE.equals(verdict.get("success"))) {
            Object errors = verdict.get("error-codes");
            if (errors instanceof java.util.Collection<?> codes
                    && (codes.contains("invalid-input-secret") || codes.contains("missing-input-secret")
                    || codes.contains("internal-error"))) {
                throw new AppException(ResultCode.CAPTCHA_UNAVAILABLE);
            }
            throw new AppException(ResultCode.CAPTCHA_INVALID);
        }
        Object hostname = verdict.get("hostname");
        if (!(hostname instanceof String host)
                || !properties.getAllowedHostnames().contains(host.toLowerCase(Locale.ROOT))
                || !params.getAction().getValue().equals(verdict.get("action"))) {
            throw new AppException(ResultCode.CAPTCHA_INVALID);
        }
    }

    private JSONObject requestVerdict(CaptchaVerifyParamsBO params) {
        JSONObject body = new JSONObject();
        body.put("secret", properties.getVerifySecret());
        body.put("response", params.getToken());
        if (properties.isBindIp()) body.put("remoteip", params.getRemoteIp());
        HttpRequest request = HttpRequest.newBuilder(properties.getVerifyUrl())
                .timeout(properties.getReadTimeout())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toJSONString(), StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) throw new AppException(ResultCode.CAPTCHA_UNAVAILABLE);
            return JSON.parseObject(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException(ResultCode.CAPTCHA_UNAVAILABLE);
        } catch (IOException | JSONException e) {
            log.warn("人机验证服务请求失败 | type={}", e.getClass().getSimpleName());
            throw new AppException(ResultCode.CAPTCHA_UNAVAILABLE);
        }
    }

    @PreDestroy
    public void close() {
        if (client != null) client.close();
    }
}
