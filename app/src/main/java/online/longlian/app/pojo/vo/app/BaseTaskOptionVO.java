package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;
import online.longlian.common.enumeration.Status;

@Data
@AllArgsConstructor
@Schema(description = "可添加到工作流的原子任务")
public class BaseTaskOptionVO {

    @JsonLongIdString
    @Schema(type = "string", description = "原子任务ID")
    private Long id;

    @Schema(description = "任务名称")
    private String name;

    @Schema(description = "Lucide 图标组件名")
    private String icon;

    @Schema(description = "任务说明")
    private String description;

    @Schema(description = "状态：ENABLED-启用，DISABLED-禁用")
    private Status status;
}
