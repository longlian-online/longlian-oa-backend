package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "任务接取结果")
public class TaskClaimResultVO {
    @Schema(description = "是否在本次接取中新增了有效的企划工坊关联；已加入时为 false")
    private boolean autoJoinedProject;
}
