package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;

@Data
@AllArgsConstructor
@Schema(description = "项目排序标题")
public class ProjectItemOrderEntryVO {
    @JsonLongIdString
    @Schema(type = "string", description = "项目ID")
    private Long id;

    @Schema(description = "项目名称")
    private String title;
}
