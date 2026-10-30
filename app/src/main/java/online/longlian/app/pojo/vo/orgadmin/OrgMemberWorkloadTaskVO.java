package online.longlian.app.pojo.vo.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;

@Data
@Builder
@Schema(description = "组员名下待提交任务")
public class OrgMemberWorkloadTaskVO {

    @JsonLongIdString
    @Schema(type = "string", description = "任务实例ID")
    private Long instanceId;

    @JsonLongIdString
    @Schema(type = "string", description = "项目ID")
    private Long itemId;

    @JsonLongIdString
    @Schema(type = "string", description = "企划ID")
    private Long projectId;

    @Schema(description = "项目名")
    private String itemTitle;

    @Schema(description = "企划名")
    private String projectName;

    @Schema(description = "阶段名")
    private String nodeName;

    @Schema(description = "本节点在所属阶段并行组中的位次，从 1 开始")
    private Integer slotIndex;

    @Schema(description = "所属阶段的任务节点数")
    private Integer slotTotal;

    @Schema(description = "同名节点跨阶段时的阶段位次；名称只出现在一个阶段时为 null")
    private Integer serialStep;

    @Schema(description = "最近的未完成前置节点名；没有前置或前置都已完成时为 null")
    private String waitingStageName;

    @Schema(description = "等待的前置节点在其并行组中的位次；可提交时为 null")
    private Integer waitingSlotIndex;

    @Schema(description = "等待的前置节点所属阶段的任务节点数；可提交时为 null")
    private Integer waitingSlotTotal;

    @Schema(description = "等待的前置节点同名跨阶段位次；可提交或名称不跨阶段时为 null")
    private Integer waitingSerialStep;
}
