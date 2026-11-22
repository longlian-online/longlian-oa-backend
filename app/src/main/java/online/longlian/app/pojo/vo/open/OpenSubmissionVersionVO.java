package online.longlian.app.pojo.vo.open;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 开放面任务提交版本。字段冻结：只增不删。
 */
@Data
@Schema(description = "开放接口任务提交版本")
public class OpenSubmissionVersionVO {

    @Schema(description = "版本号，任务实例内从 1 递增")
    private Integer versionNo;

    @Schema(description = "提交时间")
    private LocalDateTime submittedAt;

    @Schema(description = "提交人")
    private SubmitterVO submitter;

    @Schema(description = "按提交字段快照组织的值列表")
    private List<OpenTaskDetailVO.TaskFieldValueVO> values;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "提交人")
    public static class SubmitterVO {

        @Schema(description = "用户ID")
        private Long userId;

        @Schema(description = "昵称")
        private String nickname;
    }
}
