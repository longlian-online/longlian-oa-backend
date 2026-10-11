package online.longlian.app.pojo.dto.app;

import online.longlian.app.common.annotation.JsonLongIdString;

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

    @NotBlank(message = "扩展信息不能为空")
    @Schema(description = "扩展信息(JSON对象字符串)：tags 数组可为空；每项仅含 key、value，key 为非空白字符串且可重复，value 为字符串且可为空", example = "{\"tags\":[{\"key\":\"作者\",\"value\":\"smile_snow\"}]}", requiredMode = Schema.RequiredMode.REQUIRED)
    private String metadata;

    @NotNull(message = "企划简介不能为空")
    @Schema(description = "企划简介", requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;

    @JsonLongIdString
    @Schema(type = "string", description = "封面文件ID；省略时保留原封面")
    private Long coverFileId;
}
