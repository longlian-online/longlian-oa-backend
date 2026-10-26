package online.longlian.app.pojo.dto.app;

import online.longlian.app.common.annotation.JsonLongIdString;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "创建企划请求参数")
public class ProjectCreateDTO {

    @NotBlank(message = "企划名不能为空")
    @Schema(description = "企划名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotBlank(message = "别名不能为空")
    @Schema(description = "别名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String alias;

    @NotNull(message = "企划类型不能为空")
    @JsonLongIdString
    @Schema(type = "string", description = "企划类型ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long typeId;

    @NotBlank(message = "扩展信息不能为空")
    @Schema(description = "扩展信息(JSON对象字符串)：tags 数组可为空；每项仅允许 key、value、color。key 为非空白字符串且可重复；value 可省略，提供时必须为字符串且可为空；color 可省略，提供时仅允许 blue、green、amber、rose、violet。fields 可省略；提供时必须是数组且可为空，每项仅允许 name、value。name 为非空白字符串且可重复，空白 name 拒绝保存；value 必须为字符串且可为空。数组顺序和原始 JSON 保留", example = "{\"tags\":[{\"key\":\"加急\",\"color\":\"amber\"},{\"key\":\"作者\",\"value\":\"smile_snow\",\"color\":\"blue\"}],\"fields\":[{\"name\":\"作者\",\"value\":\"xxxx\"}]}", requiredMode = Schema.RequiredMode.REQUIRED)
    private String metadata;

    @NotNull(message = "企划简介不能为空")
    @Schema(description = "企划简介", requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;

    @NotNull(message = "封面不能为空")
    @JsonLongIdString
    @Schema(type = "string", description = "封面文件ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long coverFileId;

    @JsonLongIdString
    @Schema(type = "string", description = "缩略图文件ID；省略时不设置缩略图")
    private Long thumbnailFileId;
}
