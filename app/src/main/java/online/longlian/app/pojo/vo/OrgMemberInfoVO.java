package online.longlian.app.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.app.common.enumeration.Status;

import java.time.LocalDateTime;

@Data
@Schema(description = "组员信息")
public class OrgMemberInfoVO {

    @Schema(description = "成员ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "头像URL")
    private String avatarUrl;

    @Schema(description = "入组时间")
    private LocalDateTime joinedAt;

    @Schema(description = "上次提交任务时间")
    private LocalDateTime lastSubmittedAt;

    @Schema(description = "任务提交总数")
    private Integer submitCount;

    @Schema(description = "组织内角色：ORG_ADMIN-管理员，ORG_USER-普通组员")
    private String orgRole;

    @Schema(description = "成员状态：ENABLED-启用，DISABLED-禁用")
    private Status status;
}
