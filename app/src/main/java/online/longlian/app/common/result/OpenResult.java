package online.longlian.app.common.result;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.app.common.util.TraceIdUtil;

/**
 * 开放接口返回信封：站内 Result 的字段之上追加机器可读 error 标识与 requestId。
 * 仅用于 /open/**，不改变站内响应。
 */
@Data
@Schema(description = "开放接口返回结果")
// 与站内 Result 对齐：成功但无业务数据时也要输出 data:null
@JSONType(serializeFeatures = JSONWriter.Feature.WriteNulls)
public class OpenResult<T> {
    @Schema(description = "状态码：0 为成功，10xxx 为开放接口错误段")
    private int code;

    @Schema(description = "操作提示信息")
    private String msg;

    @Schema(description = "业务数据")
    private T data;

    @Schema(description = "机器可读错误标识，成功时为 null")
    private String error;

    @Schema(description = "请求追踪 ID，用于问题排查")
    private String requestId;

    private OpenResult(int code, String msg, T data, String error, String requestId) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.error = error;
        this.requestId = requestId;
    }

    public static <T> OpenResult<T> success(T data) {
        return new OpenResult<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), data, null,
                TraceIdUtil.getTraceId());
    }

    public static <T> OpenResult<T> fail(OpenResultCode resultCode) {
        return fail(resultCode, resultCode.getMsg());
    }

    public static <T> OpenResult<T> fail(OpenResultCode resultCode, String msg) {
        return new OpenResult<>(resultCode.getCode(), msg, null, resultCode.getError(), TraceIdUtil.getTraceId());
    }

    /**
     * 从已构造的响应字段组装开放信封，requestId 取当前 TraceId。供响应后置 advice 转换使用。
     */
    @SuppressWarnings("unchecked")
    public static <T> OpenResult<T> of(int code, String msg, Object data, String error) {
        return new OpenResult<>(code, msg, (T) data, error, TraceIdUtil.getTraceId());
    }
}
