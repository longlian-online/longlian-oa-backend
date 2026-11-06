package online.longlian.app.pojo.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "暂存任务草稿请求")
public class TaskDraftSaveDTO {

    @NotBlank(message = "任务修订号不能为空")
    @Pattern(regexp = "0|[1-9][0-9]{0,18}", message = "任务修订号必须为非负整数字符串")
    @DecimalMax(value = "9223372036854775806", message = "任务修订号超出范围")
    @Schema(description = "读取草稿时返回的任务修订号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String expectedTaskRevision;

    @NotBlank(message = "草稿修订号不能为空")
    @Pattern(regexp = "0|[1-9][0-9]{0,18}", message = "草稿修订号必须为非负整数字符串")
    @DecimalMax(value = "9223372036854775806", message = "草稿修订号超出范围")
    @Schema(description = "读取草稿时返回的草稿修订号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String expectedDraftRevision;

    @NotNull(message = "草稿字段值不能为空")
    @Schema(description = "按节点字段快照保存的完整表单值，允许必填字段暂缺", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Object> values;
}
