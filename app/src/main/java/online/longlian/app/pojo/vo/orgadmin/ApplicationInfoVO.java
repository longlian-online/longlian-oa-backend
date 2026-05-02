package online.longlian.app.pojo.vo.orgadmin;

import online.longlian.app.common.annotation.JsonLongIdString;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "入组申请信息")
public class ApplicationInfoVO {

    @JsonLongIdString
    @Schema(type = "string", description = "申请记录ID")
    private Long id;

    @JsonLongIdString
    @Schema(type = "string", description = "申请人用户ID")
    private Long userId;

    @Schema(description = "申请人昵称")
    private String nickname;

    @Schema(description = "申请人用户名")
    private String username;

    @Schema(description = "申请人头像URL")
    private String avatarUrl;

    @Schema(description = "申请时间")
    private LocalDateTime appliedAt;
}
