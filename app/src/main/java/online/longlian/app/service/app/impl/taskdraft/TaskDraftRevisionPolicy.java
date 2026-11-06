package online.longlian.app.service.app.impl.taskdraft;

import online.longlian.app.common.constants.TaskDraftConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.bo.app.TaskDraftRecordResultBO;

/** 不存在记录时修订号为 0；作废和已提交记录仍保留修订号，避免旧请求覆盖新一轮草稿。 */
public final class TaskDraftRevisionPolicy {

    private TaskDraftRevisionPolicy() {
    }

    public static void requireMatchingRevision(TaskDraftRecordResultBO draft, String expectedRevision) {
        String currentRevision = draft == null ? "0" : draft.getRevision().toString();
        if (!currentRevision.equals(expectedRevision)) {
            throw new AppException(ResultCode.OPERATION_FAIL, TaskDraftConstants.CONFLICT_MESSAGE);
        }
    }
}
