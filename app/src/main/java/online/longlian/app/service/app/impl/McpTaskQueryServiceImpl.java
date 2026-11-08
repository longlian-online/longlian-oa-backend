package online.longlian.app.service.app.impl;

import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.bo.app.McpTaskQueryParamsBO;
import online.longlian.app.pojo.bo.app.McpTaskQueryResultBO;
import online.longlian.app.pojo.bo.app.ProjectAttachmentListParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceDetailParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceListParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceOperateParamsBO;
import online.longlian.app.pojo.vo.app.ItemTaskFlowVO;
import online.longlian.app.pojo.vo.app.ItemTaskInstanceVO;
import online.longlian.app.pojo.vo.app.ItemTaskNodeVO;
import online.longlian.app.pojo.vo.app.TaskInstanceDetailVO;
import online.longlian.app.service.app.ItemService;
import online.longlian.app.service.app.ItemTaskFlowService;
import online.longlian.app.service.app.McpTaskQueryService;
import online.longlian.app.service.app.ProjectAttachmentService;
import online.longlian.app.service.app.ProjectService;
import online.longlian.app.service.app.TaskInstanceService;
import online.longlian.common.enumeration.TaskInstanceStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class McpTaskQueryServiceImpl implements McpTaskQueryService {

    private final ProjectService projectService;
    private final ItemService itemService;
    private final ItemTaskFlowService itemTaskFlowService;
    private final TaskInstanceService taskInstanceService;
    private final ProjectAttachmentService projectAttachmentService;

    @Override
    public McpTaskQueryResultBO.TaskList getProjectTasks(McpTaskQueryParamsBO params) {
        var project = projectService.getProjectDetail(params.projectId(), params.userId(), params.orgId());
        String itemTitle = itemService.requireItemTitle(params.projectId(), params.itemId());
        ItemTaskFlowVO flow = itemTaskFlowService.getItemTaskFlow(params.itemId(), params.orgId());
        return taskList(params, project.getTitle(), itemTitle, flow);
    }

    @Override
    public McpTaskQueryResultBO.TaskContext getTask(McpTaskQueryParamsBO params) {
        var project = projectService.getProjectDetail(params.projectId(), params.userId(), params.orgId());
        String itemTitle = itemService.requireItemTitle(params.projectId(), params.itemId());
        ItemTaskFlowVO flow = itemTaskFlowService.getItemTaskFlow(params.itemId(), params.orgId());
        ItemTaskNodeVO node = flow.getNodes().stream()
                .filter(value -> Objects.equals(value.getTaskInstanceId(), params.instanceId())).findFirst()
                .orElseThrow(() -> new AppException(ResultCode.DATA_NOT_EXIT, "该项目中不存在此任务"));
        var list = taskList(params, project.getTitle(), itemTitle, flow);
        var task = list.tasks().stream().filter(value -> params.instanceId().toString().equals(value.instanceId()))
                .findFirst().orElseThrow();
        TaskInstanceDetailVO detail = detail(params, params.instanceId());
        Map<String, Object> draftValues = task.mine()
                ? taskInstanceService.getTaskDraft(TaskInstanceOperateParamsBO.builder()
                    .instanceId(params.instanceId()).userId(params.userId()).orgId(params.orgId()).build())
                : Map.of();
        var references = projectAttachmentService.getProjectAttachments(ProjectAttachmentListParamsBO.builder()
                        .projectId(params.projectId()).orgId(params.orgId()).build()).stream()
                .map(value -> new McpTaskQueryResultBO.Attachment(value.getId().toString(), value.getFileId().toString(),
                        value.getFileName(), value.getFileMime(), value.getFileSize())).toList();
        var previous = flow.getNodes().stream()
                .filter(value -> value.getSort() < node.getSort()
                        && value.getTaskStatus() == TaskInstanceStatus.COMPLETED)
                .map(value -> new McpTaskQueryResultBO.PreviousSubmission(value.getTaskInstanceId().toString(),
                        value.getName(), detail(params, value.getTaskInstanceId()).getSubmission())).toList();
        return new McpTaskQueryResultBO.TaskContext(list, task, node.getSubmitFields(), project.getDescription(),
                project.getMetadata(), references, previous, draftValues, detail.getDraft(), detail.getSubmission());
    }

    private McpTaskQueryResultBO.TaskList taskList(McpTaskQueryParamsBO params, String projectTitle,
                                                   String itemTitle, ItemTaskFlowVO flow) {
        Map<Long, ItemTaskInstanceVO> instances = taskInstanceService.listItemTaskInstances(
                        TaskInstanceListParamsBO.builder().itemId(params.itemId()).userId(params.userId())
                                .orgId(params.orgId()).build()).stream()
                .collect(Collectors.toMap(ItemTaskInstanceVO::getId, Function.identity()));
        List<McpTaskQueryResultBO.Task> tasks = flow.getNodes().stream().map(node -> {
            ItemTaskInstanceVO instance = instances.get(node.getTaskInstanceId());
            boolean mine = instance != null && Objects.equals(instance.getAssigneeId(), params.userId());
            List<String> blockedBy = flow.getNodes().stream()
                    .filter(value -> value.getSort() < node.getSort()
                            && value.getTaskStatus() != TaskInstanceStatus.COMPLETED)
                    .map(value -> value.getName() + "（第 " + value.getSort() + " 步）").toList();
            boolean hasDraft = mine && detail(params, instance.getId()).getDraft() != null;
            return new McpTaskQueryResultBO.Task(
                    node.getTaskInstanceId() == null ? null : node.getTaskInstanceId().toString(), node.getName(),
                    node.getSort(), node.getParallelSort(), node.getTaskStatus() == null ? "LOCKED" : node.getTaskStatus().name(), node.getRevision(),
                    instance == null || instance.getAssigneeId() == null ? null : instance.getAssigneeId().toString(),
                    instance == null ? null : instance.getAssigneeNickname(), mine, hasDraft,
                    node.getTaskStatus() == TaskInstanceStatus.PENDING && blockedBy.isEmpty(),
                    mine && node.getTaskStatus() == TaskInstanceStatus.CLAIMED && blockedBy.isEmpty(), blockedBy);
        }).toList();
        return new McpTaskQueryResultBO.TaskList(params.projectId().toString(), projectTitle,
                params.itemId().toString(), itemTitle, flow.getDescription(), tasks);
    }

    private TaskInstanceDetailVO detail(McpTaskQueryParamsBO params, Long instanceId) {
        return taskInstanceService.getTaskInstanceDetail(TaskInstanceDetailParamsBO.builder()
                .instanceId(instanceId).userId(params.userId()).orgId(params.orgId()).build());
    }
}
