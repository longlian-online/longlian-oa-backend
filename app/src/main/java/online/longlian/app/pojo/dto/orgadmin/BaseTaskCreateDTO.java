package online.longlian.app.pojo.dto.orgadmin;

import online.longlian.app.common.annotation.JsonLongIdString;

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

    @JsonLongIdString
    @Schema(type = "string", description = "图标文件ID")
    private Long iconFileId;

    @Schema(description = "元数据字段定义(JSON数组)，示例：[{\"name\":\"附件\",\"fieldType\":\"file\",\"required\":true},{\"name\":\"作者\",\"fieldType\":\"text\",\"required\":true},{\"name\":\"源链接\",\"fieldType\":\"text\",\"required\":true}]")
    private String metaSchema;
}
