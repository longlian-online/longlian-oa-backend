package online.longlian.app.pojo.vo.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;

@Data
@Schema(description = "组员原子任务提交数明细")
public class OrgMemberBaseTaskSubmitCountItemVO {

    @JsonLongIdString
    @Schema(type = "string", description = "原子任务ID")
    private Long baseTaskId;

    @Schema(description = "原子任务名称")
    private String baseTaskName;

    @Schema(description = "提交数")
    private Integer submitCount;
}
