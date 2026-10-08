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

    private Task task;
    private Submission submission;

    /** Task identity and current execution state, independent of submission content. */
    public record Task(String id, String name, Integer stage, String status, Assignee assignee) { }

    /** Execution identity with an optional authorized avatar read URL. */
    public record Assignee(String id, String nickname, String avatarUrl) { }

    /** Current effective submission; reset or rejected submissions are not returned as current. */
    public record Submission(String state, LocalDateTime submittedAt, List<Field> fields) { }

    /** Ordered display items, distinct from the input form's control definitions. */
    @Schema(oneOf = {TextField.class, FileField.class}, discriminatorProperty = "type",
            discriminatorMapping = {
                    @DiscriminatorMapping(value = "text", schema = TextField.class),
                    @DiscriminatorMapping(value = "multiline", schema = TextField.class),
                    @DiscriminatorMapping(value = "file", schema = FileField.class)
            })
    public sealed interface Field permits TextField, FileField { }

    /** Server-formatted scalar content; multiline text retains submitted whitespace. */
    public record TextField(String key, String label, String type, String text) implements Field { }

    /** The single attachment submitted for a file field; null means the field was left empty. */
    public record FileField(String key, String label, String type, TaskAttachmentVO file) implements Field { }
}
