package online.longlian.app.pojo.vo.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;
import online.longlian.common.enumeration.Status;

import java.util.List;

@Data
@Builder
@Schema(description = "组员当前负载")
public class OrgMemberWorkloadVO {

    @JsonLongIdString
    @Schema(type = "string", description = "成员ID")
    private Long id;

    @JsonLongIdString
    @Schema(type = "string", description = "用户ID")
    private Long userId;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "头像URL")
    private String avatarUrl;

    @Schema(description = "成员状态：ENABLED-启用，DISABLED-禁用")
    private Status status;

    @Schema(description = "管理员备注，仅组织管理端可见")
    private String note;

    @Schema(description = "待提交任务数")
    private Integer claimedCount;

    @Schema(description = "可提交任务数")
    private Integer readyCount;

    @Schema(description = "等前置任务数")
    private Integer waitingCount;

    @Schema(description = "待提交任务，按接取时间从早到晚")
    private List<OrgMemberWorkloadTaskVO> tasks;
}
