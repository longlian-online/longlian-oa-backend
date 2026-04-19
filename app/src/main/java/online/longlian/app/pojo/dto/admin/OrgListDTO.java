package online.longlian.app.pojo.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.pojo.dto.common.PageRequestDTO;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "组织列表查询请求参数")
public class OrgListDTO extends PageRequestDTO {

    @Size(max = 32, message = "组织名长度不能超过 32 个字符")
    @Schema(description = "组织名")
    private String OrgName;

    @Schema(description = "创建时间-起始")
    private LocalDateTime startCreateTime;

    @Schema(description = "创建时间-结束")
    private LocalDateTime endCreateTime;

    @Schema(description = "排序方式")
    private SortDirection orderDir = SortDirection.DESC;
}
