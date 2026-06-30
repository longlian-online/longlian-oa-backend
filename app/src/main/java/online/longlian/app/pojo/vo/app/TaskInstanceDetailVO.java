package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "任务实例详情")
public class TaskInstanceDetailVO {

    @Schema(description = "最近一次提交的元数据(JSON字符串)，无提交记录时为 null")
    private String metadata;
}
