package online.longlian.app.common.handler;

import com.alibaba.fastjson2.JSON;
import jakarta.servlet.http.HttpServletResponse;
import online.longlian.app.common.constants.CommonConstants;
import online.longlian.app.common.result.OpenResult;
import online.longlian.app.common.result.OpenResultCode;

import java.io.IOException;

/**
 * 开放接口（/open/**）失败响应直写工具。开放面不走全站 200 惯例，按语义写真实状态码。
 */
public final class OpenApiResponseWriter {

    private OpenApiResponseWriter() {
    }

    public static void write(HttpServletResponse response, OpenResultCode code, String msg) throws IOException {
        response.setContentType(CommonConstants.CONTENT_TYPE);
        response.setStatus(code.getHttpStatus().value());
        response.getWriter().write(JSON.toJSONString(OpenResult.fail(code, msg)));
    }
}
