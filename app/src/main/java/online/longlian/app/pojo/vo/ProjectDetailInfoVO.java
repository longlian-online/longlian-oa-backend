package online.longlian.app.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.app.common.enumeration.ProjectStatus;

@Data
@Schema(description = "企划详情")
public class ProjectDetailInfoVO {

    @Schema(description = "企划ID")
    private Long id;

    @Schema(description = "企划主标题")
    private String title;

    @Schema(description = "企划别名")
    private String alias;

    @Schema(description = "封面图片URL")
    private String coverUrl;

    @Schema(description = "企划类型名称")
    private String typeName;

    @Schema(description = "原作者")
    private String author;

    @Schema(description = "企划简介")
    private String description;

    @Schema(description = "企划状态")
    private ProjectStatus status;

    @Schema(description = "整体进度百分比（0-100，基于已完成任务实例数/总任务实例数）")
    private Integer progressPercent;

    @Schema(description = "待提交任务数（CLAIMED 状态：已接取但未提交）")
    private Integer claimedTaskCount;

    @Schema(description = "待接取任务数（PENDING 状态：可领取）")
    private Integer pendingTaskCount;

    @Schema(description = "当前用户是否已将该企划添加到工坊")
    private Boolean inWorkshop;
}
