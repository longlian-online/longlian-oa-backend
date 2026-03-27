package online.longlian.app.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建/更新原子任务请求参数")
public class BaseTaskCreateDTO {

    @NotBlank(message = "任务名称不能为空")
    @Size(max = 100, message = "任务名称不能超过 100 个字符")
    @Schema(description = "任务名称（如：创建/翻译/校对）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 500, message = "任务说明不能超过 500 个字符")
    @Schema(description = "任务说明")
    private String description;

    @Schema(description = "图标文件ID")
    private Long iconFileId;

    @Schema(description = "是否需要上传文件：0-否 1-是，默认 0")
    private Byte needFile = 0;

    @Schema(description = "是否必须上传：0-否 1-是，默认 0（needFile=1 时生效）")
    private Byte requiredFile = 0;

    @Schema(description = "允许的文件 MIME 类型（逗号分隔，如 image/jpeg,application/pdf）")
    private String allowedMimeTypes;
}
