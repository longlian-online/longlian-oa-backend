package online.longlian.app.pojo.vo.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;

import java.util.List;

@Data
@Builder
@Schema(description = "组员当前接取的任务")
public class OrgMemberClaimedTaskVO {

    @JsonLongIdString
    @Schema(type = "string", description = "成员ID")
    private Long memberId;

    @JsonLongIdString
    @Schema(type = "string", description = "用户ID")
    private Long userId;

    @Schema(description = "全部任务数")
    private Integer totalCount;

    @Schema(description = "进行中任务数：前置阶段已全部完成，当前可提交")
    private Integer submittableCount;

    @Schema(description = "等待中任务数：存在未完成的前置阶段")
    private Integer waitingCount;

    @Schema(description = "任务列表，按接取时间从早到晚")
    private List<OrgMemberClaimedTaskItemVO> list;
}
