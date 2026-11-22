package online.longlian.app.pojo.vo.open;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 开放面任务广场企划分组。字段冻结：只增不删。
 */
@Data
@Schema(description = "开放接口任务广场企划分组")
public class OpenPlazaProjectGroupVO {

    @Schema(description = "企划ID")
    private Long projectId;

    @Schema(description = "企划标题")
    private String title;

    @Schema(description = "企划类型名")
    private String typeName;

    @Schema(description = "封面读取链接，按次签发，短期有效；无封面为空")
    private String coverUrl;

    @Schema(description = "缩略图读取链接，按次签发，短期有效；无缩略图为空")
    private String thumbnailUrl;

    @Schema(description = "缺人任务数")
    private Integer pendingCount;

    @Schema(description = "涉及项目数")
    private Integer itemCount;

    @Schema(description = "缺人任务列表")
    private List<OpenPlazaTaskVO> tasks;
}
