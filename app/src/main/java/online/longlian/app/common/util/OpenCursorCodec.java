package online.longlian.app.common.util;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.bo.open.OpenCursorBO;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 开放接口游标编解码。格式 v1:{afterId}:{afterUpdatedAtMillis|x}，Base64 传输。
 * 游标只做定位，不做加密；内容被篡改时按参数错误拒绝。
 */
public final class OpenCursorCodec {

    private static final String VERSION = "v1";
    private static final String NO_TIMESTAMP = "x";

    private OpenCursorCodec() {
    }

    public static String encode(OpenCursorBO cursor) {
        String payload = VERSION + ":" + cursor.afterId() + ":"
                + (cursor.afterUpdatedAtMillis() == null ? NO_TIMESTAMP : cursor.afterUpdatedAtMillis());
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public static OpenCursorBO decode(String cursor) {
        String payload;
        try {
            payload = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new AppException(ResultCode.PARAM_ERROR, "游标格式错误");
        }
        String[] parts = payload.split(":");
        if (parts.length != 3 || !VERSION.equals(parts[0])) {
            throw new AppException(ResultCode.PARAM_ERROR, "游标格式错误");
        }
        try {
            Long afterId = Long.parseLong(parts[1]);
            Long afterUpdatedAtMillis = NO_TIMESTAMP.equals(parts[2]) ? null : Long.parseLong(parts[2]);
            return new OpenCursorBO(afterId, afterUpdatedAtMillis);
        } catch (NumberFormatException e) {
            throw new AppException(ResultCode.PARAM_ERROR, "游标格式错误");
        }
    }
}
