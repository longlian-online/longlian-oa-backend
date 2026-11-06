package online.longlian.app.common.constants;

public final class TaskDraftConstants {

    /** 复用操作失败状态码；客户端据此提示重新加载，并保留本地未暂存内容。 */
    public static final String CONFLICT_MESSAGE = "草稿或任务已更新，请重新加载后重试";

    private TaskDraftConstants() {
    }
}
