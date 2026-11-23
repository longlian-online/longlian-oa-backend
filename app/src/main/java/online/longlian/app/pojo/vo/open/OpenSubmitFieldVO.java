package online.longlian.app.pojo.vo.open;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 开放面任务流节点提交字段的表单快照（服务端定义，调用方按 key 提交值）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "开放接口提交字段定义")
public class OpenSubmitFieldVO {

    @Schema(description = "字段键，提交时 values 的键")
    private String key;

    @Schema(description = "展示名")
    private String label;

    @Schema(description = "字段类型：text/textarea/file 等")
    private String type;

    @Schema(description = "是否必填")
    private Boolean required;

    @Schema(description = "选项值（选择类字段）")
    private java.util.List<String> options;
}
