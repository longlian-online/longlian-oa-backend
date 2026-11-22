package online.longlian.app.service.open;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.mapper.ItemMapper;
import online.longlian.app.mapper.ItemTaskNodeMapper;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.mapper.TaskSubmissionMapper;
import online.longlian.app.pojo.bo.app.taskinstance.ItemStageNodeStatusBO;
import online.longlian.app.pojo.bo.app.taskinstance.TaskInstanceDetailParamsBO;
import online.longlian.app.pojo.bo.app.taskinstance.TaskInstanceDetailResultBO;
import online.longlian.app.pojo.bo.app.taskinstance.TaskSubmissionVersionsParamsBO;
import online.longlian.app.pojo.bo.app.taskinstance.TaskSubmissionVersionResultBO;
import online.longlian.app.pojo.bo.open.OpenSubmissionFieldBO;
import online.longlian.app.pojo.bo.open.OpenSubmissionFileBO;
import online.longlian.app.pojo.bo.open.OpenSubmissionVersionBO;
import online.longlian.app.pojo.bo.app.taskinstance.TaskAttachmentResultBO;
import online.longlian.app.pojo.bo.app.taskinstance.TaskInstanceDetailParamsBO;
import online.longlian.app.pojo.bo.open.OpenTaskDetailBO;
import online.longlian.app.pojo.bo.open.OpenTaskDetailParamsBO;
import online.longlian.app.pojo.bo.open.OpenTaskParticipantBO;
import online.longlian.app.pojo.bo.open.OpenTaskSubmissionBO;
import online.longlian.app.pojo.bo.open.OpenTaskSubmissionsParamsBO;
import online.longlian.app.pojo.entity.Item;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.app.pojo.entity.TaskSubmission;
import online.longlian.app.service.app.taskinstance.TaskInstanceService;
import online.longlian.app.service.app.taskstage.SlotLabels;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 开放面任务实例查询。
 * 跨模块只读（standard #9）：开放面是跨领域的查询域，详情的提交字段与附件展示
 * 复用站内 {@link TaskInstanceService}（含 item→project→org 归属校验，跨组织一律 404）；
 * 标识、槽位、版本数是本域查询所需的一次性补充读取，不在任务域另设无业务含义的方法。
 */
@Service
@RequiredArgsConstructor
public class OpenTaskQueryServiceImpl implements OpenTaskQueryService {

    private final TaskInstanceService taskInstanceService;
    private final TaskInstanceMapper taskInstanceMapper;
    private final TaskSubmissionMapper taskSubmissionMapper;
    private final ItemTaskNodeMapper itemTaskNodeMapper;
    private final ItemMapper itemMapper;
    private final ProjectMapper projectMapper;

    @Override
    public OpenTaskDetailBO getTaskDetail(OpenTaskDetailParamsBO params) {
        // 不传 userId：开放面详情不返回接取人暂存
        TaskInstanceDetailResultBO detail = taskInstanceService.getTaskInstanceDetail(
                TaskInstanceDetailParamsBO.builder()
                        .instanceId(params.instanceId())
                        .orgId(params.orgId())
                        .build());

        TaskInstance instance = taskInstanceMapper.selectById(params.instanceId());
        ItemTaskNode node = itemTaskNodeMapper.selectById(instance.getItemTaskNodeId());
        Item item = itemMapper.selectById(instance.getItemId());
        Project project = projectMapper.selectById(instance.getProjectId());
        SlotLabels.Position slot = SlotLabels.of(instance.getItemTaskNodeId(),
                taskInstanceMapper.selectItemStageNodeStatuses(List.of(instance.getItemId())));
        Long submissionCount = taskSubmissionMapper.selectCount(new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getTaskInstanceId, params.instanceId()));

        TaskInstanceDetailResultBO.TaskSummaryResultBO task = detail.getTask();
        TaskInstanceDetailResultBO.TaskSubmissionResultBO submission = detail.getSubmission();
        return new OpenTaskDetailBO(
                params.instanceId(),
                task.name(),
                task.stage(),
                slot.slotIndex(),
                slot.slotTotal(),
                slot.serialStep(),
                instance.getItemId(),
                item == null ? null : item.getTitle(),
                instance.getProjectId(),
                project == null ? null : project.getTitle(),
                instance.getStatus(),
                instance.getRevision(),
                toParticipant(task.assignee()),
                new OpenTaskSubmissionBO(
                        submission.state(), submission.submittedAt(), toFields(submission.fields())),
                submissionCount == null ? 0 : submissionCount);
    }

    @Override
    public List<OpenSubmissionVersionBO> listSubmissions(OpenTaskSubmissionsParamsBO params) {
        List<TaskSubmissionVersionResultBO> versions = taskInstanceService.listSubmissionVersions(
                TaskSubmissionVersionsParamsBO.builder()
                        .instanceId(params.instanceId())
                        .orgId(params.orgId())
                        .build());
        return versions.stream()
                .map(version -> new OpenSubmissionVersionBO(
                        version.getVersionNo(),
                        version.getSubmittedAt(),
                        toParticipant(version.getSubmitter()),
                        toFields(version.getFields())))
                .toList();
    }

    private OpenTaskParticipantBO toParticipant(TaskInstanceDetailResultBO.TaskAssigneeResultBO assignee) {
        if (assignee == null) {
            return null;
        }
        return new OpenTaskParticipantBO(parseUserId(assignee.id()), assignee.nickname());
    }

    private OpenTaskParticipantBO toParticipant(TaskSubmissionVersionResultBO.TaskParticipantResultBO participant) {
        if (participant == null) {
            return null;
        }
        return new OpenTaskParticipantBO(parseUserId(participant.id()), participant.nickname());
    }

    /** 站内展示层把用户 ID 序列化为字符串以避免大数精度丢失，开放面统一还原为数值。 */
    private Long parseUserId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<OpenSubmissionFieldBO> toFields(List<TaskInstanceDetailResultBO.TaskDisplayFieldResultBO> fields) {
        if (fields == null) {
            return List.of();
        }
        return fields.stream().<OpenSubmissionFieldBO>map(field -> switch (field) {
            case TaskInstanceDetailResultBO.TaskTextFieldResultBO text -> new OpenSubmissionFieldBO(
                    text.key(), text.label(), text.type(), text.text(), null);
            case TaskInstanceDetailResultBO.TaskFileFieldResultBO file -> new OpenSubmissionFieldBO(
                    file.key(), file.label(), file.type(), null, toFile(file.file()));
        }).toList();
    }

    private OpenSubmissionFileBO toFile(TaskAttachmentResultBO attachment) {
        if (attachment == null) {
            return null;
        }
        return new OpenSubmissionFileBO(attachment.getId(), attachment.getName(), attachment.getSizeText());
    }
}
