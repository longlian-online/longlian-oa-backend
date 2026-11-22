package online.longlian.app.pojo.dto.open;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "开放接口提交任务请求")
public class OpenTaskSubmitDTO {

    @NotNull(message = "提交内容不能为空")
    @Schema(description = "按节点 metaSchema 提交字段定义填写的值；文件字段传 {\"fileId\":...}",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Object> values;
}
