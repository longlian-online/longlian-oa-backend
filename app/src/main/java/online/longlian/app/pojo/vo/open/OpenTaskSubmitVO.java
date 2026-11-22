package online.longlian.app.pojo.vo.open;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "开放接口提交任务响应")
public class OpenTaskSubmitVO {

    @Schema(description = "本次提交的版本号，任务实例内从 1 递增")
    private int versionNo;
}
