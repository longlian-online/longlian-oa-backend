package online.longlian.app.pojo.dto.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import online.longlian.app.pojo.dto.common.PageRequestDTO;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "加入组织邀请码列表查询参数")
public class JoinInviteCodeListDTO extends PageRequestDTO {

    @Schema(description = "状态筛选：PENDING 待使用，USED 已用尽；不传返回尚未手动失效的全部邀请码")
    private String status;
}
