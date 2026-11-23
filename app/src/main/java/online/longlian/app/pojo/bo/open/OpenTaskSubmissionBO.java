package online.longlian.app.pojo.bo.open;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务当前生效的提交快照；state 为 submitted / not_submitted，与站内口径一致。
 */
public record OpenTaskSubmissionBO(String state, LocalDateTime submittedAt, List<OpenSubmissionFieldBO> values) {
}
