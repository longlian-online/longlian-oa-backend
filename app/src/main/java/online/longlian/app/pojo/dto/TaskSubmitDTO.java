package online.longlian.app.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "提交任务请求参数")
public class TaskSubmitDTO {

    @Schema(description = "上传文件ID（原子任务 needFile=1 时必填）")
    private Long fileId;

    @Schema(description = "附加元数据（JSON字符串，可选）")
    private String metadata;
}
