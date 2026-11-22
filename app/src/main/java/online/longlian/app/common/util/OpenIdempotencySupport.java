package online.longlian.app.common.util;

import jakarta.servlet.http.HttpServletRequest;
import online.longlian.app.common.constants.OpenApiConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.bo.open.IdempotencyCheckParamsBO;
import online.longlian.common.enumeration.IdempotencySource;

/**
 * 开放面写接口的幂等上下文组装：幂等键来自请求头，请求体哈希来自缓存过滤器写入的请求属性。
 */
public final class OpenIdempotencySupport {

    private OpenIdempotencySupport() {
    }

    /**
     * 组装查重入参。写接口必须携带幂等键，缺失即参数错误。
     */
    public static IdempotencyCheckParamsBO buildParams(HttpServletRequest request, IdempotencySource source,
                                                      Long callerId) {
        String idempotencyKey = request.getHeader(OpenApiConstants.IDEMPOTENCY_KEY_HEADER);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new AppException(ResultCode.PARAM_ERROR, "写接口必须携带 X-Idempotency-Key");
        }
        if (idempotencyKey.length() > 64) {
            throw new AppException(ResultCode.PARAM_ERROR, "X-Idempotency-Key 过长");
        }
        String bodyHash = (String) request.getAttribute(OpenApiConstants.BODY_HASH_ATTRIBUTE);
        if (bodyHash == null) {
            // 缓存过滤器已覆盖 /open/** 全部非 GET 请求，到不了这里说明链路被破坏
            throw new AppException(ResultCode.FAIL, "幂等上下文缺失");
        }
        return new IdempotencyCheckParamsBO(source, callerId, idempotencyKey,
                request.getMethod(), request.getServletPath(), bodyHash);
    }
}
