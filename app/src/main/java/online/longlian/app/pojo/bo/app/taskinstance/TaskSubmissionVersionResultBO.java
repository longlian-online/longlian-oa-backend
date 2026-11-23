package online.longlian.app.pojo.bo.app.taskinstance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 单个提交版本：版本号、生命周期状态、提交人/审核人与展示字段。
 * fields 复用详情的字段结构；unreadable 表示存量数据不再符合当前表单契约。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSubmissionVersionResultBO {

    private Integer versionNo;

    private String status;

    private LocalDateTime submittedAt;

    private TaskParticipantResultBO submitter;

    private LocalDateTime reviewedAt;

    private TaskParticipantResultBO reviewer;

    private String reviewComment;

    private List<TaskInstanceDetailResultBO.TaskDisplayFieldResultBO> fields;

    private boolean unreadable;

    /** 参与人：提交人或打回人，附可选的已授权头像读取链接。 */
    public record TaskParticipantResultBO(String id, String nickname, String avatarUrl) {
    }
}
