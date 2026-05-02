package online.longlian.app.pojo.vo.admin;

import online.longlian.app.common.annotation.JsonLongIdString;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "组织简要信息")
public class OrgSimpleInfoVO {

    @JsonLongIdString
    @Schema(type = "string", description = "组织ID")
    private Long id;

    @Schema(description = "组织名称")
    private String name;

    @Schema(description = "组织头像文件url")
    private String avatarUrl;
}
