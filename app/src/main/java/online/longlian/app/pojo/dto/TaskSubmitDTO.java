package online.longlian.app.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "提交任务请求参数")
public class TaskSubmitDTO {

    @Schema(description = "提交元数据(JSON对象)。示例：{\"values\":{\"attachment\":{\"fileId\":123},\"author\":\"张三\"}}")
    private String metadata;
}
