package online.longlian.app.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "流程模板选项")
public class TaskTemplateOptionVO {

    @Schema(description = "流程模板ID")
    private Long id;

    @Schema(description = "流程模板名称")
    private String name;
}
