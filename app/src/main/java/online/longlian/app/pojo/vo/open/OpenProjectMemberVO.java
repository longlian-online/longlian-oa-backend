package online.longlian.app.pojo.vo.open;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 开放面企划成员。字段冻结：只增不删；不含头像。
 */
@Data
@Schema(description = "开放接口企划成员")
public class OpenProjectMemberVO {

    @Schema(description = "成员用户ID")
    private Long userId;

    @Schema(description = "成员昵称")
    private String nickname;

    @Schema(description = "加入企划时间")
    private LocalDateTime joinedAt;

    @Schema(description = "是否企划创建人")
    private boolean creator;

    @Schema(description = "有效完成任务数，打回/重置不计")
    private int completedCount;

    @Schema(description = "最近一次有效提交时间")
    private LocalDateTime lastSubmittedAt;
}
