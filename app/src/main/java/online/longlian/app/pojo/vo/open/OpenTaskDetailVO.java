package online.longlian.app.pojo.vo.open;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.common.enumeration.TaskInstanceStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 开放面任务实例详情。字段冻结：只增不删；附件只含名称与大小。
 */
@Data
@Schema(description = "开放接口任务实例详情")
public class OpenTaskDetailVO {

    @Schema(description = "任务实例ID")
    private Long taskInstanceId;

    @Schema(description = "任务名称")
    private String name;

    @Schema(description = "阶段顺序")
    private Integer stage;

    @Schema(description = "同阶段槽位序号，从 1 开始")
    private Integer slotIndex;

    @Schema(description = "同阶段槽位总数")
    private Integer slotTotal;

    @Schema(description = "同名任务跨阶段出现的位次，仅一个阶段时为空")
    private Integer serialStep;

    @Schema(description = "所属项目ID")
    private Long itemId;

    @Schema(description = "所属项目标题")
    private String itemTitle;

    @Schema(description = "所属企划ID")
    private Long projectId;

    @Schema(description = "所属企划标题")
    private String projectTitle;

    @Schema(description = "任务状态：PENDING/CLAIMED/COMPLETED")
    private TaskInstanceStatus status;

    @Schema(description = "任务修订号")
    private Long revision;

    @Schema(description = "接取人，未接取为空")
    private TaskAssigneeVO assignee;

    @Schema(description = "当前生效的提交")
    private TaskSubmissionVO submission;

    @Schema(description = "提交版本数（含已打回/已重置）")
    private long submissionCount;

    @Data
    @Schema(description = "任务参与人")
    public static class TaskAssigneeVO {

        @Schema(description = "用户ID")
        private Long userId;

        @Schema(description = "昵称")
        private String nickname;

        public TaskAssigneeVO() {
        }

        public TaskAssigneeVO(Long userId, String nickname) {
            this.userId = userId;
            this.nickname = nickname;
        }
    }

    @Data
    @NoArgsConstructor
    @Schema(description = "当前提交")
    public static class TaskSubmissionVO {

        @Schema(description = "提交状态：submitted/not_submitted")
        private String state;

        @Schema(description = "提交时间，未提交为空")
        private LocalDateTime submittedAt;

        @Schema(description = "按提交字段快照组织的值列表")
        private List<TaskFieldValueVO> values;

        public TaskSubmissionVO(String state, LocalDateTime submittedAt, List<TaskFieldValueVO> values) {
            this.state = state;
            this.submittedAt = submittedAt;
            this.values = values == null ? null : List.copyOf(values);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "提交字段值；text 与 file 二选一")
    public static class TaskFieldValueVO {

        @Schema(description = "字段键")
        private String key;

        @Schema(description = "展示名")
        private String label;

        @Schema(description = "字段类型")
        private String type;

        @Schema(description = "文本值")
        private String text;

        @Schema(description = "文件值，未填写为空")
        private TaskFileValueVO file;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "提交文件元信息，无读取链接")
    public static class TaskFileValueVO {

        @Schema(description = "文件ID")
        private String fileId;

        @Schema(description = "文件名")
        private String name;

        @Schema(description = "文件大小（已格式化）")
        private String size;
    }
}
