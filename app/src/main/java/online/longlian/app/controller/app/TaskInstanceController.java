package online.longlian.app.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.annotation.ResponseMessage;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.enumeration.OrganizationDeclaration;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.pojo.bo.app.taskinstance.TaskClaimResultBO;
import online.longlian.app.pojo.bo.app.taskinstance.TaskInstanceDetailParamsBO;
import online.longlian.app.pojo.bo.app.taskinstance.TaskInstanceListParamsBO;
import online.longlian.app.pojo.bo.app.taskinstance.TaskInstanceOperateParamsBO;
import online.longlian.app.pojo.bo.app.taskinstance.TaskInstanceAbandonParamsBO;
import online.longlian.app.pojo.bo.app.taskinstance.TaskInstanceAssignParamsBO;
import online.longlian.app.pojo.bo.app.taskinstance.TaskInstanceRejectParamsBO;
import online.longlian.app.pojo.bo.app.taskinstance.TaskInstanceSubmitParamsBO;
import online.longlian.app.pojo.bo.app.taskinstance.TaskInstanceProxySubmitParamsBO;
import online.longlian.app.pojo.dto.app.TaskProxySubmitDTO;
import online.longlian.app.pojo.bo.app.mytask.MyTaskParamsBO;
import online.longlian.app.pojo.bo.app.taskplaza.TaskPlazaParamsBO;
import online.longlian.app.pojo.bo.common.PageParamsBO;
import online.longlian.app.pojo.dto.app.MyTaskListDTO;
import online.longlian.app.pojo.dto.app.TaskSubmitDTO;
import online.longlian.app.pojo.dto.app.TaskAbandonDTO;
import online.longlian.app.pojo.dto.app.TaskAssignDTO;

import online.longlian.app.pojo.dto.app.TaskPlazaListDTO;
import online.longlian.app.pojo.dto.app.TaskRejectDTO;
import online.longlian.app.pojo.bo.app.taskinstance.TaskSubmissionVersionsParamsBO;
import online.longlian.app.pojo.bo.app.taskinstance.ItemTaskInstanceResultBO;
import online.longlian.app.pojo.bo.app.taskinstance.TaskAttachmentResultBO;
import online.longlian.app.pojo.bo.app.taskinstance.TaskInstanceDetailResultBO;
import online.longlian.app.pojo.bo.app.taskinstance.TaskSubmissionVersionResultBO;
import online.longlian.app.pojo.bo.app.mytask.MyTaskResultBO;
import online.longlian.app.pojo.bo.app.taskplaza.PlazaProjectGroupResultBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.vo.app.ItemTaskInstanceVO;
import online.longlian.app.pojo.vo.app.MyTaskVO;
import online.longlian.app.pojo.vo.app.PlazaProjectGroupVO;
import online.longlian.app.pojo.vo.app.PlazaTaskVO;
import online.longlian.app.pojo.vo.app.TaskAttachmentVO;
import online.longlian.app.pojo.vo.app.TaskClaimResultVO;
import online.longlian.app.pojo.bo.app.project.ProjectAttachmentDownloadResultBO;
import online.longlian.app.pojo.bo.app.taskinstance.TaskAttachmentDownloadParamsBO;
import online.longlian.app.pojo.vo.app.ProjectAttachmentDownloadVO;
import online.longlian.app.pojo.vo.app.TaskInstanceDetailVO;
import online.longlian.app.pojo.vo.app.TaskSubmissionVersionVO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.service.app.MyTaskService;
import online.longlian.app.service.app.TaskInstanceService;
import online.longlian.app.service.app.TaskPlazaService;
import online.longlian.app.service.orgadmin.BaseTaskService;
import online.longlian.app.pojo.vo.app.PlazaTaskTypeOptionVO;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "任务实例接口", description = "任务接取、分配、放弃、提交、代提交、重置、打回、查看详情、按项目查询实例列表")
@RequestMapping("/app/task/instance")
@RestController
@RequiredArgsConstructor
public class TaskInstanceController {

    private final TaskInstanceService taskInstanceService;
    private final TaskPlazaService taskPlazaService;
    private final BaseTaskService baseTaskService;
    private final MyTaskService myTaskService;

    @Operation(
            summary = "我的待办",
            description = "分页返回当前用户在当前组织内已接取（CLAIMED）的任务。" +
                    "bucket=SUBMITTABLE 前置阶段全部完成，WAITING 存在未完成前置；默认按接取时间升序"
    )
    @GetMapping("/mine")
    @ResponseMessage("查询成功")
    public PageResultVO<MyTaskVO> listMyTasks(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @ModelAttribute @Valid MyTaskListDTO listDTO) {
        PageResultBO<MyTaskResultBO> resultBO = myTaskService.listMyTasks(
                MyTaskParamsBO.builder()
                        .userId(sessionContext.userId())
                        .orgId(sessionContext.orgId())
                        .bucket(listDTO.getBucket())
                        .sortDirection(listDTO.getSortDirection())
                        .page(new PageParamsBO(listDTO.getPageNum(), listDTO.getPageSize()))
                        .build());

        List<MyTaskVO> myTaskVOList = resultBO.getList().stream().map(bo -> {
            MyTaskVO myTaskVO = new MyTaskVO();
            BeanUtils.copyProperties(bo, myTaskVO);
            return myTaskVO;
        }).toList();
        return new PageResultVO<>(myTaskVOList, resultBO.getTotal());
    }


    @Operation(
            summary = "任务广场（缺人任务）",
            description = "分页返回当前组织内可接取的缺人任务，按企划分组。" +
                    "stageScope=ALL 返回所有阶段，CURRENT（默认）仅返回首个未完成阶段"
    )
    @GetMapping("/plaza")
    @ResponseMessage("查询成功")
    public PageResultVO<PlazaProjectGroupVO> getTaskPlaza(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @ModelAttribute @Valid TaskPlazaListDTO listDTO) {
        PageResultBO<PlazaProjectGroupResultBO> resultBO = taskPlazaService.getTaskPlaza(
                TaskPlazaParamsBO.builder()
                        .orgId(sessionContext.orgId())
                        .keyword(listDTO.getKeyword())
                        .projectType(listDTO.getProjectType())
                        .baseTaskId(listDTO.getBaseTaskId())
                        .sortBy(listDTO.getSortBy())
                        .stageScope(listDTO.getStageScope())
                        .page(new PageParamsBO(listDTO.getPageNum(), listDTO.getPageSize()))
                        .build());

        List<PlazaProjectGroupVO> groupVOList = resultBO.getList().stream().map(bo -> {
            PlazaProjectGroupVO groupVO = new PlazaProjectGroupVO();
            BeanUtils.copyProperties(bo, groupVO, "tasks");
            groupVO.setTasks(bo.getTasks().stream().map(taskBO -> {
                PlazaTaskVO taskVO = new PlazaTaskVO();
                BeanUtils.copyProperties(taskBO, taskVO);
                return taskVO;
            }).toList());
            return groupVO;
        }).toList();
        return new PageResultVO<>(groupVOList, resultBO.getTotal());
    }

    @Operation(summary = "任务大厅任务类型选项", description = "当前组织启用的原子任务及已有流程仍引用的停用原子任务")
    @GetMapping("/plaza/task-types")
    public List<PlazaTaskTypeOptionVO> listPlazaTaskTypes(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext) {
        return baseTaskService.listPlazaTaskTypes(sessionContext.orgId()).stream()
                .map(option -> new PlazaTaskTypeOptionVO(option.id(), option.name()))
                .toList();
    }

    @Operation(
            summary = "查询项目下的任务实例列表",
            description = "返回当前项目所有已生成的任务实例"
    )
    @Parameter(name = "itemId", description = "项目ID")
    @GetMapping("/item/{itemId}")
    @ResponseMessage("查询成功")
    public List<ItemTaskInstanceVO> listItemTaskInstances(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
                                                           @PathVariable Long itemId) {
        List<ItemTaskInstanceResultBO> resultBOList = taskInstanceService.listItemTaskInstances(
                TaskInstanceListParamsBO.builder()
                        .itemId(itemId)
                        .userId(sessionContext.userId())
                        .orgId(sessionContext.orgId())
                        .build());
        return resultBOList.stream().map(bo -> {
            ItemTaskInstanceVO itemTaskInstanceVO = new ItemTaskInstanceVO();
            BeanUtils.copyProperties(bo, itemTaskInstanceVO);
            return itemTaskInstanceVO;
        }).toList();
    }

    @Operation(
            summary = "查看任务实例详情",
            description = "返回任务信息及服务器组装的提交字段和附件"
    )
    @Parameter(name = "instanceId", description = "任务实例ID")
    @GetMapping("/{instanceId}/detail")
    @ResponseMessage("查询成功")
    public TaskInstanceDetailVO getTaskInstanceDetail(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
                                                       @PathVariable Long instanceId) {
        TaskInstanceDetailResultBO resultBO = taskInstanceService.getTaskInstanceDetail(
                TaskInstanceDetailParamsBO.builder()
                        .instanceId(instanceId)
                        .orgId(sessionContext.orgId())
                        .userId(sessionContext.userId())
                        .build());
        return toTaskInstanceDetailVO(resultBO);
    }

    @Operation(summary = "获取任务附件读取链接", description = "点击预览或下载时新取签名，详情和版本列表不返回读取地址")
    @Parameter(name = "instanceId", description = "任务实例ID")
    @Parameter(name = "fileId", description = "文件ID")
    @GetMapping("/{instanceId}/attachments/{fileId}/download")
    @ResponseMessage("获取成功")
    public ProjectAttachmentDownloadVO getTaskAttachmentDownload(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @PathVariable Long instanceId,
            @PathVariable Long fileId) {
        ProjectAttachmentDownloadResultBO result = taskInstanceService.getTaskAttachmentDownload(
                TaskAttachmentDownloadParamsBO.builder()
                        .instanceId(instanceId)
                        .fileId(fileId)
                        .orgId(sessionContext.orgId())
                        .build());
        ProjectAttachmentDownloadVO vo = new ProjectAttachmentDownloadVO();
        vo.setReadUrl(result.getReadUrl());
        vo.setExpireAt(result.getExpireAt());
        return vo;
    }

    @Operation(
            summary = "任务提交版本列表",
            description = "按版本号倒序返回该任务实例的全部提交版本，包含提交人/打回人与按表单快照组织的字段和附件"
    )
    @Parameter(name = "instanceId", description = "任务实例ID")
    @GetMapping("/{instanceId}/submissions")
    @ResponseMessage("查询成功")
    public List<TaskSubmissionVersionVO> listSubmissionVersions(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @PathVariable Long instanceId) {
        List<TaskSubmissionVersionResultBO> resultBOList = taskInstanceService.listSubmissionVersions(
                TaskSubmissionVersionsParamsBO.builder()
                        .instanceId(instanceId)
                        .orgId(sessionContext.orgId())
                        .build());
        return resultBOList.stream()
                .map(this::toTaskSubmissionVersionVO)
                .toList();
    }

    @Operation(
        summary = "接取任务",
        description = "接取 PENDING 状态的任务，接取后任务状态变为 CLAIMED，归属当前登录用户；未加入企划时自动加入，返回本次是否新增关联"
    )
    @Parameter(name = "instanceId", description = "任务实例ID")
    @PostMapping("/{instanceId}/claim")
    @ResponseMessage("接取成功")
    public TaskClaimResultVO claimTask(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
                                      @PathVariable Long instanceId) {
        TaskClaimResultBO result = taskInstanceService.claimTask(
                TaskInstanceOperateParamsBO.builder()
                        .instanceId(instanceId)
                        .userId(sessionContext.userId())
                        .orgId(sessionContext.orgId())
                        .build());
        return new TaskClaimResultVO(result.isAutoJoinedProject());
    }

    @Operation(
        summary = "分配任务",
        description = "企划负责人将 PENDING 任务直接分配给当前组织的启用组员。" +
                "分配后任务变为 CLAIMED 并归属该组员，组员无需再领取；同时发送站内信和邮件"
    )
    @Parameter(name = "instanceId", description = "任务实例ID")
    @PostMapping("/{instanceId}/assign")
    @ResponseMessage("分配成功")
    public TaskClaimResultVO assignTask(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
                                         @PathVariable Long instanceId,
                                         @RequestBody @Valid TaskAssignDTO request) {
        TaskClaimResultBO result = taskInstanceService.assignTask(
                TaskInstanceAssignParamsBO.builder()
                        .instanceId(instanceId)
                        .userId(sessionContext.userId())
                        .orgId(sessionContext.orgId())
                        .assigneeId(request.getAssigneeId())
                        .expectedRevision(request.getExpectedRevision())
                        .build());
        return new TaskClaimResultVO(result.isAutoJoinedProject());
    }

    @Operation(
        summary = "放弃任务",
        description = "接取人可主动放弃；企划创建人可取消他人接取。须匹配 revision，且后续阶段没有完成任务"
    )
    @Parameter(name = "instanceId", description = "任务实例ID")
    @PostMapping("/{instanceId}/abandon")
    @ResponseMessage("已放弃")
    public void abandonTask(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
                             @PathVariable Long instanceId,
                             @RequestBody @Valid TaskAbandonDTO request) {
        taskInstanceService.abandonTask(
                TaskInstanceAbandonParamsBO.builder()
                        .instanceId(instanceId)
                        .userId(sessionContext.userId())
                        .orgId(sessionContext.orgId())
                        .expectedRevision(request.getExpectedRevision())
                        .build());
    }

    @Operation(
        summary = "提交任务",
        description = "提交待提交（CLAIMED）的任务，提交后任务状态变为 COMPLETED。提交内容通过 values 传递"
    )
    @Parameter(name = "instanceId", description = "任务实例ID")
    @PostMapping("/{instanceId}/submit")
    @ResponseMessage("提交成功")
    public void submitTask(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
                            @PathVariable Long instanceId,
                            @RequestBody @Valid TaskSubmitDTO taskSubmitDTO) {
        taskInstanceService.submitTask(
                TaskInstanceSubmitParamsBO.builder()
                        .instanceId(instanceId)
                        .userId(sessionContext.userId())
                        .orgId(sessionContext.orgId())
                        .values(taskSubmitDTO.getValues())
                        .build());
    }

    @Operation(summary = "读取暂存", description = "返回当前接取人的暂存字段；没有暂存时值为空")
    @GetMapping("/{instanceId}/draft")
    @ResponseMessage("查询成功")
    public online.longlian.app.pojo.vo.app.TaskDraftValuesVO getTaskDraft(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @PathVariable Long instanceId) {
        return new online.longlian.app.pojo.vo.app.TaskDraftValuesVO(taskInstanceService.getTaskDraft(
                TaskInstanceOperateParamsBO.builder()
                        .instanceId(instanceId)
                        .userId(sessionContext.userId())
                        .orgId(sessionContext.orgId())
                        .build()));
    }

    @Operation(summary = "暂存任务", description = "覆盖当前接取人的一条暂存提交，不改变任务状态、提交计数和阶段解锁")
    @PutMapping("/{instanceId}/draft")
    @ResponseMessage("暂存成功")
    public Long saveTaskDraft(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
                              @PathVariable Long instanceId,
                              @RequestBody @Valid TaskSubmitDTO taskSubmitDTO) {
        return taskInstanceService.saveTaskDraft(TaskInstanceSubmitParamsBO.builder()
                .instanceId(instanceId)
                .userId(sessionContext.userId())
                .orgId(sessionContext.orgId())
                .values(taskSubmitDTO.getValues())
                .build());
    }

    @Operation(summary = "提交暂存", description = "把当前暂存提交转为正式提交")
    @PostMapping("/{instanceId}/draft/submit")
    @ResponseMessage("提交成功")
    public void submitTaskDraft(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
                                 @PathVariable Long instanceId) {
        taskInstanceService.submitTaskDraft(TaskInstanceOperateParamsBO.builder()
                .instanceId(instanceId)
                .userId(sessionContext.userId())
                .orgId(sessionContext.orgId())
                .build());
    }

    @Operation(
            summary = "代提交任务",
            description = "仅企划负责人可将尚未接取的任务直接记为指定组员已提交。"
                    + "组员必须是当前组织的启用成员。提交时间使用请求中的设定值，不使用当前时间。已接取或已提交的任务不可调用"
    )
    @Parameter(name = "instanceId", description = "任务实例ID")
    @PostMapping("/{instanceId}/proxy-submit")
    @ResponseMessage("代提交成功")
    public void proxySubmitTask(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
                                 @PathVariable Long instanceId,
                                 @RequestBody @Valid TaskProxySubmitDTO request) {
        taskInstanceService.proxySubmitTask(TaskInstanceProxySubmitParamsBO.builder()
                .instanceId(instanceId)
                .userId(sessionContext.userId())
                .orgId(sessionContext.orgId())
                .assigneeId(request.getAssigneeId())
                .submittedAt(request.getSubmittedAt())
                .expectedRevision(request.getExpectedRevision())
                .values(request.getValues())
                .build());
    }

    @Operation(
        summary = "重置任务提交",
        description = "将已完成（COMPLETED）的任务重置为 CLAIMED 状态，允许重新提交。"
    )
    @Parameter(name = "instanceId", description = "任务实例ID")
    @PostMapping("/{instanceId}/reset")
    @ResponseMessage("已重置")
    public void resetTask(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
                           @PathVariable Long instanceId) {
        taskInstanceService.resetTask(
                TaskInstanceOperateParamsBO.builder()
                        .instanceId(instanceId)
                        .userId(sessionContext.userId())
                        .orgId(sessionContext.orgId())
                        .build());
    }

    @Operation(
        summary = "打回任务",
        description = "企划创建人或下一阶段执行人可将已完成任务打回，保留接取人与提交历史。" +
                "须匹配 revision 并填写打回意见，且后续阶段没有完成任务"
    )
    @Parameter(name = "instanceId", description = "任务实例ID")
    @PostMapping("/{instanceId}/reject")
    @ResponseMessage("已打回")
    public void rejectTask(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
                            @PathVariable Long instanceId,
                            @RequestBody @Valid TaskRejectDTO taskRejectDTO) {
        taskInstanceService.rejectTask(
                TaskInstanceRejectParamsBO.builder()
                        .instanceId(instanceId)
                        .userId(sessionContext.userId())
                        .orgId(sessionContext.orgId())
                        .expectedRevision(taskRejectDTO.getExpectedRevision())
                        .reviewComment(taskRejectDTO.getReviewComment())
                        .build());
    }

    private TaskInstanceDetailVO toTaskInstanceDetailVO(TaskInstanceDetailResultBO resultBO) {
        TaskInstanceDetailVO detailVO = new TaskInstanceDetailVO();
        TaskInstanceDetailResultBO.TaskSummaryResultBO task = resultBO.getTask();
        if (task != null) {
            detailVO.setTask(new TaskInstanceDetailVO.TaskSummaryVO(
                    task.id(), task.name(), task.stage(), task.status(), toTaskAssigneeVO(task.assignee())));
        }
        TaskInstanceDetailResultBO.TaskSubmissionResultBO submission = resultBO.getSubmission();
        if (submission != null) {
            detailVO.setSubmission(new TaskInstanceDetailVO.TaskSubmissionVO(
                    submission.state(), submission.submittedAt(), toDisplayFieldVOs(submission.fields())));
        }
        TaskInstanceDetailResultBO.TaskDraftResultBO draft = resultBO.getDraft();
        if (draft != null) {
            detailVO.setDraft(new TaskInstanceDetailVO.TaskDraftVO(
                    draft.updatedAt(), toDisplayFieldVOs(draft.fields()), draft.unreadable()));
        }
        return detailVO;
    }

    private TaskInstanceDetailVO.TaskAssigneeVO toTaskAssigneeVO(
            TaskInstanceDetailResultBO.TaskAssigneeResultBO assigneeBO) {
        if (assigneeBO == null) {
            return null;
        }
        return new TaskInstanceDetailVO.TaskAssigneeVO(
                assigneeBO.id(), assigneeBO.nickname(), assigneeBO.avatarUrl());
    }

    private List<TaskInstanceDetailVO.TaskDisplayFieldVO> toDisplayFieldVOs(
            List<TaskInstanceDetailResultBO.TaskDisplayFieldResultBO> fields) {
        if (fields == null) {
            return List.of();
        }
        return fields.stream().<TaskInstanceDetailVO.TaskDisplayFieldVO>map(field -> switch (field) {
            case TaskInstanceDetailResultBO.TaskTextFieldResultBO textField ->
                    new TaskInstanceDetailVO.TaskTextFieldVO(
                            textField.key(), textField.label(), textField.type(), textField.text());
            case TaskInstanceDetailResultBO.TaskFileFieldResultBO fileField ->
                    new TaskInstanceDetailVO.TaskFileFieldVO(
                            fileField.key(), fileField.label(), fileField.type(),
                            toTaskAttachmentVO(fileField.file()));
        }).toList();
    }

    private TaskAttachmentVO toTaskAttachmentVO(TaskAttachmentResultBO attachmentBO) {
        if (attachmentBO == null) {
            return null;
        }
        TaskAttachmentVO attachmentVO = new TaskAttachmentVO();
        BeanUtils.copyProperties(attachmentBO, attachmentVO);
        return attachmentVO;
    }

    private TaskSubmissionVersionVO toTaskSubmissionVersionVO(TaskSubmissionVersionResultBO resultBO) {
        TaskSubmissionVersionVO versionVO = new TaskSubmissionVersionVO();
        BeanUtils.copyProperties(resultBO, versionVO, "submitter", "reviewer", "fields");
        versionVO.setSubmitter(toTaskParticipantVO(resultBO.getSubmitter()));
        versionVO.setReviewer(toTaskParticipantVO(resultBO.getReviewer()));
        versionVO.setFields(toDisplayFieldVOs(resultBO.getFields()));
        return versionVO;
    }

    private TaskSubmissionVersionVO.TaskParticipantVO toTaskParticipantVO(
            TaskSubmissionVersionResultBO.TaskParticipantResultBO participantBO) {
        if (participantBO == null) {
            return null;
        }
        return new TaskSubmissionVersionVO.TaskParticipantVO(
                participantBO.id(), participantBO.nickname(), participantBO.avatarUrl());
    }

}
