package online.longlian.app.common.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
/**
 * 通用返回结果封装类
 * 泛型T：业务数据的类型（如UserVO、List<UserVO>、PageResult<UserVO>、Boolean等）
 */
@Data
@Schema(description = "通用返回结果")
public class Result<T> {
    @Schema(description = "状态码：2000成功，4xxx客户端错误，5xxx服务端错误，1xxx业务错误")
    private int code;

    @Schema(description = "操作提示信息")
    private String msg;

    @Schema(description = "业务数据（成功时返回，失败时为null）")
    private T data;

    private Result() {}

    private Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), data);
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