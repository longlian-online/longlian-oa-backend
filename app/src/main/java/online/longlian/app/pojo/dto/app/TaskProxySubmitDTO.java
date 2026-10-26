package online.longlian.app.pojo.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Schema(description = "代提交任务请求参数")
public class TaskProxySubmitDTO {

    @NotNull(message = "组员不能为空")
    @Positive(message = "组员不能为空")
    @JsonLongIdString
    @Schema(type = "string", description = "归属组员的用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long assigneeId;

    @NotNull(message = "提交时间不能为空")
    @Schema(type = "string", example = "2026-10-01 12:00:00", description = "记入提交记录的时间，不能晚于当前时间",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime submittedAt;

    @NotBlank(message = "任务修订号不能为空")
    @Schema(description = "打开代提交时看到的任务修订号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String expectedRevision;

    @NotNull(message = "提交内容不能为空")
    @Schema(description = "按服务器提交字段定义填写的值；文件仅传 fileId", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Object> values;
}
