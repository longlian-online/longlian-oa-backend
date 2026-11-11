package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/** Read model assembled for display without exposing task storage encoding. */
@Data
@Schema(description = "任务实例详情")
public class TaskInstanceDetailVO {

    private TaskSummaryVO task;
    private TaskSubmissionVO submission;
    /** 仅当前接取人可见；没有暂存时为空。 */
    private TaskDraftVO draft;

    /** Task identity and current execution state, independent of submission content. */
    public record TaskSummaryVO(String id, String name, Integer stage, String status, TaskAssigneeVO assignee) { }

    /** Execution identity with an optional authorized avatar read URL. */
    public record TaskAssigneeVO(String id, String nickname, String avatarUrl) { }

    /** Current effective submission; reset or rejected submissions are not returned as current. */
    public record TaskSubmissionVO(String state, LocalDateTime submittedAt, List<TaskDisplayFieldVO> fields) { }

    /** 接取人尚未正式提交的暂存。字段结构和版本记录相同。 */
    public record TaskDraftVO(LocalDateTime updatedAt, List<TaskDisplayFieldVO> fields, boolean unreadable) { }

    /** Ordered display items, distinct from the input form's control definitions. */
    @Schema(oneOf = {TaskTextFieldVO.class, TaskFileFieldVO.class}, discriminatorProperty = "type",
            discriminatorMapping = {
                    @DiscriminatorMapping(value = "text", schema = TaskTextFieldVO.class),
                    @DiscriminatorMapping(value = "multiline", schema = TaskTextFieldVO.class),
                    @DiscriminatorMapping(value = "file", schema = TaskFileFieldVO.class)
            })
    public sealed interface TaskDisplayFieldVO permits TaskTextFieldVO, TaskFileFieldVO { }

    /** Server-formatted scalar content; multiline text retains submitted whitespace. */
    public record TaskTextFieldVO(String key, String label, String type, String text) implements TaskDisplayFieldVO { }

    /** The single attachment submitted for a file field; null means the field was left empty. */
    public record TaskFileFieldVO(String key, String label, String type, TaskAttachmentVO file) implements TaskDisplayFieldVO { }
}
