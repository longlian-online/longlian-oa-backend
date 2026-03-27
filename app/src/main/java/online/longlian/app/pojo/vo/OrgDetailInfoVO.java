package online.longlian.app.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.app.common.enumeration.Status;

import java.time.LocalDateTime;
@NoArgsConstructor
@Data
@AllArgsConstructor
@Schema(description = "组织详细信息")
public class OrgDetailInfoVO {

    @Schema(description = "组织ID")
    private Long id;

    @Schema(description = "组织名称")
    private String name;

    @Schema(description = "组织头像文件url")
    private String avatarUrl;

    @Schema(description = "组织简介")
    private String description;

    @Schema(description = "组织状态 1-启用 0-禁用")
    private Status status;

    @Schema(description = "组织创建时间")
    private LocalDateTime createdAt;
}
