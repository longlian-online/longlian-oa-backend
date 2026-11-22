package online.longlian.common.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import online.longlian.common.annotation.ModelEnum;

/**
 * OpenAPI 密钥状态。吊销行保留审计，不做物理删除。
 */
@Getter
@AllArgsConstructor
@ModelEnum(model = "open_api_key", field = "status")
public enum OpenApiKeyStatus implements CodeEnum {
    REVOKED(0, "吊销"),
    ENABLED(1, "启用");

    private final Integer code;
    private final String desc;
}
