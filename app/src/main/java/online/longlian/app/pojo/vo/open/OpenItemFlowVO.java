package online.longlian.app.pojo.vo.open;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 开放面项目任务流。字段冻结：只增不删。
 * status 渲染中文描述（进行中/已完成），与站内 ProjectStatus 的 @JsonValue 口径一致。
 */
@Data
@Schema(description = "开放接口项目任务流")
public class OpenItemFlowVO {

    @Schema(description = "项目标题")
    private String itemTitle;

    @Schema(description = "项目状态：进行中/已完成")
    private String status;

    @Schema(description = "完成时间，进行中为空")
    private LocalDateTime completedAt;

    @Schema(description = "是否已有提交记录")
    private Boolean hasSubmissionRecord;

    @Schema(description = "节点列表，按阶段与并行组顺序")
    private List<OpenItemFlowNodeVO> nodes;
}
