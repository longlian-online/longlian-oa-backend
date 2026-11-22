package online.longlian.app.pojo.bo.open;

/**
 * 开放面创建上传资源入参。业务类型由服务端固定为 task_submit，不随请求变化。
 */
public record OpenFileCreateParamsBO(Long userId, Long orgId,
                                     String fileName, String fileExt,
                                     Long fileSize, String fileMime) {
}
