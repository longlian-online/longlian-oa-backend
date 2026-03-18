package online.longlian.app.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "组织简要信息")
public class OrgSimpleInfoVO {

    @Schema(description = "组织ID")
    private Long id;

    @Schema(description = "组织名称")
    private String name;

    @Schema(description = "组织头像文件url")
    private String avatarUrl;
}
