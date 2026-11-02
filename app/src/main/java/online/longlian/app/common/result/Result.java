package online.longlian.app.common.result;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
/**
 * 通用返回结果封装类
 * 泛型T：业务数据的类型（如UserVO、List<UserVO>、PageResult<UserVO>、Boolean等）
 */
@Data
@Schema(description = "通用返回结果")
// 成功但无业务数据时也要输出 data:null，否则前端拿到的是 undefined 而不是 null。
@JSONType(serializeFeatures = JSONWriter.Feature.WriteNulls)
public class Result<T> {
    @Schema(description = "状态码：0 为成功，非0即异常")
    private int code;

    @Schema(description = "操作提示信息")
    private String msg;

    @Schema(description = "业务数据（成功时返回，失败时为null）")
    private T data;

    private Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> Result<T> success(String msg) {
        return new Result<>(ResultCode.SUCCESS.getCode(), msg, null);
    }
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), msg, data);
    }
    public static <T> Result<T> fail(int code ,String msg) {
        return new Result<>(code, msg, null);
    }
    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMsg(),null);
    }
}