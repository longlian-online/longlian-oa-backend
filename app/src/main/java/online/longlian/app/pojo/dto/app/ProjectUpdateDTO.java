package online.longlian.app.pojo.dto.app;

import online.longlian.app.common.annotation.JsonLongIdString;
import online.longlian.common.enumeration.ProjectStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "修改企划请求参数")
public class ProjectUpdateDTO {

    @NotBlank(message = "企划名不能为空")
    @Schema(description = "企划名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotBlank(message = "别名不能为空")
    @Schema(description = "别名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String alias;

    @JsonLongIdString
    @Schema(type = "string", description = "企划类型ID；省略时保留原类型")
    private Long typeId;

    @NotBlank(message = "扩展信息不能为空")
    @Schema(description = "扩展信息(JSON对象字符串)：tags 数组可为空；每项仅允许 key、value、color。key 为非空白字符串且可重复；value 可省略，提供时必须为字符串且可为空；color 可省略，提供时仅允许 blue、green、amber、rose、violet。数组顺序和原始 JSON 保留", example = "{\"tags\":[{\"key\":\"加急\",\"color\":\"amber\"},{\"key\":\"作者\",\"value\":\"smile_snow\",\"color\":\"blue\"}]}", requiredMode = Schema.RequiredMode.REQUIRED)
    private String metadata;

    @NotNull(message = "企划简介不能为空")
    @Schema(description = "企划简介", requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;

    @JsonLongIdString
    @Schema(type = "string", description = "封面文件ID；省略时保留原封面")
    private Long coverFileId;

    @JsonLongIdString
    @Schema(type = "string", description = "缩略图文件ID；省略时保留原缩略图")
    private Long thumbnailFileId;

    @Schema(description = "目标状态：进行中/已完成；已归档不允许选择，不传则保持原状态")
    private ProjectStatus status;
}
