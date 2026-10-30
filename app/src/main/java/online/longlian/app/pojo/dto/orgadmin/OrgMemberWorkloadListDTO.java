package online.longlian.app.pojo.dto.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.pojo.dto.common.PageRequestDTO;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "组员负载查询请求参数")
public class OrgMemberWorkloadListDTO extends PageRequestDTO {

    @Size(max = 50, message = "搜索关键词长度不能超过 50 个字符")
    @Schema(description = "昵称模糊搜索关键词")
    private String keyword;

    @Schema(description = "按待提交任务数排序，默认从多到少；数量相同时按成员 ID 升序")
    private SortDirection orderDir = SortDirection.DESC;
}
