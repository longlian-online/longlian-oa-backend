package online.longlian.app.pojo.vo.orgadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import online.longlian.app.common.annotation.JsonLongIdString;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "组员各原子任务提交数")
public class OrgMemberBaseTaskSubmitCountVO {

    @JsonLongIdString
    @Schema(type = "string", description = "成员ID")
    private Long memberId;

    @JsonLongIdString
    @Schema(type = "string", description = "用户ID")
    private Long userId;

    @Schema(description = "任务提交总数")
    private Integer totalSubmitCount;

    @Schema(description = "各原子任务提交数列表；返回组织下所有原子任务，无提交记录的原子任务提交数为 0")
    private List<OrgMemberBaseTaskSubmitCountItemVO> list;
}
