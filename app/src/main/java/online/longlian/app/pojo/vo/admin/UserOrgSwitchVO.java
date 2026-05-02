package online.longlian.app.pojo.vo.admin;

import online.longlian.app.common.annotation.JsonLongIdString;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户切换的组织信息")
public class UserOrgSwitchVO {

    @JsonLongIdString
    @Schema(type = "string", description = "组织ID")
    private Long id;

    @Schema(description = "组织名称")
    private String name;

    @Schema(description = "组织头像文件url")
    private String avatarUrl;

    @Schema(description = "在组织内的角色列表")
    private List<String> roles;
}
