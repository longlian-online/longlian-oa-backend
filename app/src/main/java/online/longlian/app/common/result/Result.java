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

    /**
     * 操作成功，无业务数据
     * return Result.success();
     */
    public static <T> Result<T> success() {
        // 复用带数据的方法，data传null
        return success(null);
    }
    /**
     * 操作成功，带业务数据
     * return Result.success(userVO); return Result.success(userList);
     * @param data 业务数据（泛型T）
     */
    public static <T> Result<T> success(T data) {
        // 使用ResultCode.SUCCESS的默认状态码（2000）和提示（操作成功）
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), data);
    }

    /**
     * 操作成功，自定义提示信息+业务数据（如“添加用户成功”）
     * return Result.success("添加用户成功", userVO);
     * @param msg 自定义提示信息
     * @param data 业务数据
     */
    public static <T> Result<T> success(String msg, T data) {
        // 状态码仍用2000，提示信息自定义
        return new Result<>(ResultCode.SUCCESS.getCode(), msg, data);
    }

    /**
     * 操作失败，自定义提示信息
     * return Result.fail("密码格式错误");
     * @param msg 自定义提示信息
     */
    public static <T> Result<T> fail(String msg) {
        // 状态码用ResultCode.FAIL（5000），提示信息自定义
        return new Result<>(ResultCode.FAIL.getCode(), msg, null);
    }
}