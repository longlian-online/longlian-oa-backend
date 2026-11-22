package online.longlian.app.pojo.vo.open;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 开放面企划下项目条目。字段冻结：只增不删；不含节点结构（走任务流接口）。
 * status 渲染中文描述（进行中/已完成），与站内 ProjectStatus 的 @JsonValue 口径一致。
 */
@Data
@Schema(description = "开放接口项目条目")
public class OpenProjectItemVO {

    @Schema(description = "项目ID")
    private Long id;

    @Schema(description = "项目标题")
    private String title;

    @Schema(description = "状态：进行中/已完成")
    private String status;

    @Schema(description = "当前推进到的节点名，无任务节点时为空")
    private String currentNodeName;

    @Schema(description = "完成节点占比（0-100）")
    private Integer progressPercent;

    @Schema(description = "是否已有提交记录（含已打回/已重置）")
    private Boolean hasSubmissionRecord;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间，作为增量拉取依据")
    private LocalDateTime updatedAt;
}
