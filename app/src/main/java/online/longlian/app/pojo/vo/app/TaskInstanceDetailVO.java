package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "任务实例详情")
public class TaskInstanceDetailVO {

    private Task task;
    private Submission submission;

    public record Task(String id, String name, Integer stage, String status, Assignee assignee) { }

    public record Assignee(String id, String nickname, String avatarUrl) { }

    public record Submission(String state, LocalDateTime submittedAt, List<Field> fields) { }

    @Schema(oneOf = {TextField.class, FilesField.class}, discriminatorProperty = "type",
            discriminatorMapping = {
                    @DiscriminatorMapping(value = "text", schema = TextField.class),
                    @DiscriminatorMapping(value = "multiline", schema = TextField.class),
                    @DiscriminatorMapping(value = "files", schema = FilesField.class)
            })
    public sealed interface Field permits TextField, FilesField { }

    public record TextField(String key, String label, String type, String text) implements Field { }

    public record FilesField(String key, String label, String type, List<TaskAttachmentVO> files,
                             String emptyText) implements Field { }
}
