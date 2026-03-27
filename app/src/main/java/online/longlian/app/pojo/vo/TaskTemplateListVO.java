package online.longlian.app.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.app.common.enumeration.Status;

import java.time.LocalDateTime;

@Data
@Schema(description = "任务模板列表行信息")
public class TaskTemplateListVO {

    @Schema(description = "模板ID")
    private Long id;

    @Schema(description = "模板名称")
    private String name;

    @Schema(description = "模板说明")
    private String description;

    @Schema(description = "状态：ENABLED-启用，DISABLED-禁用")
    private Status status;

    @Schema(description = "引用次数（已基于该模板创建的任务流数量）")
    private Integer refCount;

    @Schema(description = "创建人ID")
    private Long creatorId;

    @Schema(description = "创建人昵称")
    private String creatorNickname;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
