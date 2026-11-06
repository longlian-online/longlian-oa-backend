package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "草稿所属任务")
public class TaskDraftTaskVO {

    @Schema(description = "任务实例ID")
    private String instanceId;

    @Schema(description = "项目ID")
    private String itemId;

    @Schema(description = "任务当前修订号")
    private String revision;

    @Schema(description = "任务状态")
    private String status;
}
