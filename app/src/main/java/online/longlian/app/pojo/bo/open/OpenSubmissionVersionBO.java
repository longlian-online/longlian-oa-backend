package online.longlian.app.pojo.bo.open;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 开放面任务提交版本：版本号倒序全量返回；不含打回人/审核意见等管理视角字段。
 */
public record OpenSubmissionVersionBO(Integer versionNo, LocalDateTime submittedAt,
                                     OpenTaskParticipantBO submitter, List<OpenSubmissionFieldBO> values) {
}
