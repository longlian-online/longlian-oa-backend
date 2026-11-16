package online.longlian.app.pojo.bo.app.taskinstance;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务实例详情：服务层组装的可展示读取模型，不暴露任务存储编码。
 */
@Data
public class TaskInstanceDetailResultBO {

    private TaskSummaryResultBO task;
    private TaskSubmissionResultBO submission;
    /** 仅当前接取人可见；没有暂存时为空。 */
    private TaskDraftResultBO draft;

    /** 任务身份与当前执行状态，与提交内容无关。 */
    public record TaskSummaryResultBO(String id, String name, Integer stage, String status, TaskAssigneeResultBO assignee) { }

    /** 执行人身份，附带可选的已授权头像读取链接。 */
    public record TaskAssigneeResultBO(String id, String nickname, String avatarUrl) { }

    /** 当前生效的提交；已重置或已打回的提交不作为当前提交返回。 */
    public record TaskSubmissionResultBO(String state, LocalDateTime submittedAt, List<TaskDisplayFieldResultBO> fields) { }

    /** 接取人尚未正式提交的暂存。字段结构和版本记录相同。 */
    public record TaskDraftResultBO(LocalDateTime updatedAt, List<TaskDisplayFieldResultBO> fields, boolean unreadable) { }

    /** 有序展示项，区别于输入表单的控件定义。 */
    public sealed interface TaskDisplayFieldResultBO permits TaskTextFieldResultBO, TaskFileFieldResultBO { }

    /** 服务端格式化的标量内容；多行文本保留提交时的空白。 */
    public record TaskTextFieldResultBO(String key, String label, String type, String text) implements TaskDisplayFieldResultBO { }

    /** 文件字段对应的单个附件；null 表示该字段未填写。 */
    public record TaskFileFieldResultBO(String key, String label, String type, TaskAttachmentResultBO file)
            implements TaskDisplayFieldResultBO { }
}
