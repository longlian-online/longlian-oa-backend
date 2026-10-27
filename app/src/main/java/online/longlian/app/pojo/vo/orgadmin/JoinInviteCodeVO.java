package online.longlian.app.pojo.vo.orgadmin;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import online.longlian.app.common.annotation.JsonLongIdString;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "加入组织邀请码")
public class JoinInviteCodeVO {

    @JsonLongIdString
    @Schema(type = "string", description = "邀请记录 ID")
    private Long id;

    @Schema(description = "邀请码")
    private String inviteCode;

    @Schema(description = "已使用次数")
    private Integer useCount;

    @Schema(description = "使用上限，为空表示不限次数")
    private Integer maxUses;

    @Schema(description = "状态：PENDING 待使用，USED 已用尽")
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
