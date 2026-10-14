package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.app.common.annotation.JsonLongIdString;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "我的待办条目")
public class MyTaskVO {

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

    @Schema(description = "阶段名（任务节点名称）")
    private String nodeName;

    @Schema(description = "阶段序号（节点 sort）")
    private Integer stageSort;

    @Schema(description = "企划名")
    private String projectName;

    @Schema(description = "接取时间")
    private LocalDateTime claimedAt;

    @Schema(description = "等待的前置阶段名（sort 最大的未完成前置）；仅 WAITING 分组返回，否则为 null")
    private String waitingStageName;
}
