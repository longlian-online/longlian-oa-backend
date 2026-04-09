package online.longlian.app.common.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户操作类型
 */
@Getter
@AllArgsConstructor
public enum UserOperationType implements CodeEnum {

    TASK_CLAIM(1, "接取任务"),
    TASK_SUBMIT(2, "提交任务"),
    TASK_ABANDON(3, "放弃任务"),
    TASK_REJECT(4, "打回任务"),
    TASK_RESET(5, "重置任务"),
    FILE_DOWNLOAD(6, "下载任务");

    private final Integer code;
    private final String desc;

    @Override
    public Integer getCode() {
        return code;
    }
}
