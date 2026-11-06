package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "当前暂存内容")
public class TaskDraftValuesVO {

    @Schema(description = "已暂存的字段值；没有暂存时为空对象")
    private Map<String, Object> values;
}
