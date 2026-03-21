package online.longlian.app.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import online.longlian.app.common.enumeration.ProjectStatus;

@Data
@Schema(description = "修改企划请求参数")
public class ProjectUpdateDTO {

    @NotBlank(message = "企划名不能为空")
    @Schema(description = "企划名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotBlank(message = "别名不能为空")
    @Schema(description = "别名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String alias;

    @NotNull(message = "作者不能为空")
    @Schema(description = "原作者/作者信息", requiredMode = Schema.RequiredMode.REQUIRED)
    private String author;

    @NotNull(message = "企划简介不能为空")
    @Schema(description = "企划简介", requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;

    @NotNull(message = "企划状态不能为空")
    @Schema(description = "企划状态，1-进行中 2-已完成 3-已归档", requiredMode = Schema.RequiredMode.REQUIRED)
    private ProjectStatus projectStatus;

    @NotNull(message = "封面不能为空")
    @Schema(description = "封面文件ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long coverFileId;
}