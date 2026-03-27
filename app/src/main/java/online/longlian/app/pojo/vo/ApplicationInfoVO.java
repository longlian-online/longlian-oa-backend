package online.longlian.app.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "入组申请信息")
public class ApplicationInfoVO {

    @Schema(description = "申请记录ID")
    private Long id;

    @Schema(description = "申请人用户ID")
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
