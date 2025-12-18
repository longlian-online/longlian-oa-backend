package online.longlian.app.dto;
import lombok.Getter;

@Getter
public enum ResultCode {

    // ====================== 通用状态码（自定义，原HTTP状态码×10）======================
    /** 操作成功（通用成功） */
    SUCCESS(2000, "操作成功"),
    /** 操作失败（通用失败，兜底用） */
    FAIL(5000, "操作失败"),
    /** 参数校验失败（如@Valid校验不通过） */
    PARAM_ERROR(4000, "参数校验失败"),
    /** 未授权（如未登录、token过期） */
    UNAUTHORIZED(4010, "未授权，请先登录"),
    /** 禁止访问（如权限不足） */
    FORBIDDEN(4030, "禁止访问，权限不足"),
    /** 资源不存在（如查询ID不存在） */
    NOT_FOUND(4040, "请求资源不存在"),
    /** 请求方式错误（如POST用成GET） */
    METHOD_NOT_ALLOWED(4050, "请求方式错误"),
    /** 服务降级/过载保护 */
    SERVICE_UNAVAILABLE(5030, "服务暂不可用，请稍后重试");

    private final int code;
    /** 状态码对应的提示信息 */
    private final String msg;
    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}