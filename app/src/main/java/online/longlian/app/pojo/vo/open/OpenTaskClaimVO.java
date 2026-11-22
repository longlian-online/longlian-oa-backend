package online.longlian.app.pojo.vo.open;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "开放接口接取任务响应")
public class OpenTaskClaimVO {

    @Schema(description = "接取时未加入企划是否已自动加入")
    private boolean autoJoinedProject;
}
