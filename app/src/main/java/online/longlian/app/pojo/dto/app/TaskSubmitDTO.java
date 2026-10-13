package online.longlian.app.pojo.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

@Data
@Schema(description = "提交任务请求参数")
public class TaskSubmitDTO {

    @NotNull
    @Schema(description = "按服务器提交字段定义填写的值；文件仅传 fileId", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Object> values;
}
