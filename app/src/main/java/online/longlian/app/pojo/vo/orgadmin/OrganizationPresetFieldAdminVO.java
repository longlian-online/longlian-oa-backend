package online.longlian.app.pojo.vo.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;

import java.time.LocalDateTime;

@Data
@Schema(description = "组织预置表单字段管理列表信息")
public class OrganizationPresetFieldAdminVO {

    @JsonLongIdString
    @Schema(type = "string", description = "组织预置表单字段ID")
    private Long id;

    @Schema(description = "字段名")
    private String name;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "创建人昵称，空白时为空")
    private String creatorNickname;
}
