package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 单个提交版本视图：版本号、生命周期状态、提交人/审核人与展示字段。
 * fields 复用详情的字段结构；unreadable 表示存量数据不再符合当前表单契约。
 */
@Data
@Schema(description = "任务提交版本")
public class TaskSubmissionVersionVO {

    @Schema(description = "版本号，任务实例内从 1 递增")
    private Integer versionNo;

    @Schema(description = "版本状态：SUBMITTED 已提交 / REJECTED 已打回 / RESET 已重置")
    private String status;

    @Schema(description = "提交时间")
    private LocalDateTime submittedAt;

    @Schema(description = "提交人")
    private Person submitter;

    @Schema(description = "打回时间，仅 REJECTED 存在")
    private LocalDateTime reviewedAt;

    @Schema(description = "打回人，仅 REJECTED 存在")
    private Person reviewer;

    @Schema(description = "打回意见，仅 REJECTED 存在")
    private String reviewComment;

    @Schema(description = "按表单快照组织的展示字段")
    private List<TaskInstanceDetailVO.Field> fields;

    @Schema(description = "存量数据内容解析失败；fields 为空不代表该版本没有内容")
    private boolean unreadable;

    /** 参与人：提交人或打回人，附可选的已授权头像读取链接。 */
    public record Person(String id, String nickname, String avatarUrl) {
    }
}
