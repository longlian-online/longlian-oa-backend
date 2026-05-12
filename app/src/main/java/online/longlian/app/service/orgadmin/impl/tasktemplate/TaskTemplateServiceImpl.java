package online.longlian.app.service.orgadmin.impl.tasktemplate;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.TaskTemplateMapper;
import online.longlian.app.mapper.TaskTemplateNodeMapper;
import online.longlian.app.pojo.bo.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateChangeStatusParamsBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateCreateParamsBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateDetailResultBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateListResultBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateNodeCreateParamsBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateUpdateParamsBO;
import online.longlian.app.pojo.entity.TaskTemplate;
import online.longlian.app.pojo.entity.TaskTemplateNode;
import online.longlian.app.service.orgadmin.TaskTemplateService;
import online.longlian.common.enumeration.Status;
import online.longlian.common.enumeration.TaskTemplateScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskTemplateServiceImpl implements TaskTemplateService {

    private final TaskTemplateMapper taskTemplateMapper;
    private final TaskTemplateNodeMapper taskTemplateNodeMapper;
    private final Clock clock;
    private final TaskTemplateQueryBuilder taskTemplateQueryBuilder;
    private final TaskTemplateAssembler taskTemplateAssembler;

    @Override
    public PageResultBO<TaskTemplateListResultBO> listTaskTemplates(TaskTemplateListParamsBO params) {
        Page<TaskTemplate> page = new Page<>(params.getPage().getPageNum(), params.getPage().getPageSize());
        Page<TaskTemplate> templatePage = taskTemplateMapper.selectPage(page, taskTemplateQueryBuilder.buildListQuery(params));
        List<TaskTemplate> templates = templatePage.getRecords();
        if (templates.isEmpty()) {
            return new PageResultBO<>(Collections.emptyList(), templatePage.getTotal());
        }
        return new PageResultBO<>(taskTemplateAssembler.assembleList(templates), templatePage.getTotal());
    }

    @Override
    public TaskTemplateDetailResultBO getTaskTemplateDetail(Long templateId, Long orgId) {
        TaskTemplate template = validateAndGetTemplate(templateId, orgId);
        return taskTemplateAssembler.assembleDetail(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTaskTemplate(TaskTemplateCreateParamsBO params) {
        LocalDateTime now = LocalDateTime.now(clock);
        TaskTemplate template = TaskTemplate.builder()
                .orgId(params.getOrgId())
                .name(params.getName())
                .description(params.getDescription())
                .status(Status.ENABLED)
                .scope(TaskTemplateScope.ORGANIZATION)
                .refCount(0)
                .creatorId(params.getCreatorId())
                .createdAt(now)
                .updatedAt(now)
                .build();
        taskTemplateMapper.insert(template);

        List<TaskTemplateNode> nodes = params.getNodes().stream()
                .map(node -> buildNode(template.getId(), node, now))
                .toList();
        for (TaskTemplateNode node : nodes) {
            taskTemplateNodeMapper.insert(node);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTaskTemplate(TaskTemplateUpdateParamsBO params) {
        validateAndGetTemplate(params.getTemplateId(), params.getOrgId());
        LocalDateTime now = LocalDateTime.now(clock);
        taskTemplateMapper.update(null,
                new LambdaUpdateWrapper<TaskTemplate>()
                        .eq(TaskTemplate::getId, params.getTemplateId())
                        .set(TaskTemplate::getName, params.getName())
                        .set(TaskTemplate::getDescription, params.getDescription())
                        .set(TaskTemplate::getUpdatedAt, now)
        );

        taskTemplateNodeMapper.update(null,
                new LambdaUpdateWrapper<TaskTemplateNode>()
                        .eq(TaskTemplateNode::getTaskTemplateId, params.getTemplateId())
                        .isNull(TaskTemplateNode::getDeletedAt)
                        .set(TaskTemplateNode::getDeletedAt, now)
        );

        List<TaskTemplateNode> newNodes = params.getNodes().stream()
                .map(node -> buildNode(params.getTemplateId(), node, now))
                .toList();
        for (TaskTemplateNode node : newNodes) {
            taskTemplateNodeMapper.insert(node);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeTaskTemplateStatus(TaskTemplateChangeStatusParamsBO params) {
        TaskTemplate template = taskTemplateMapper.selectById(params.getTemplateId());
        if (template == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "任务模板不存在");
        }
        if (!template.getOrgId().equals(params.getOrgId())) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "无权操作该任务模板");
        }
        taskTemplateMapper.update(null,
                new LambdaUpdateWrapper<TaskTemplate>()
                        .eq(TaskTemplate::getId, template.getId())
                        .set(TaskTemplate::getStatus, params.getStatus())
        );
    }

    private TaskTemplateNode buildNode(Long templateId, TaskTemplateNodeCreateParamsBO node, LocalDateTime now) {
        return TaskTemplateNode.builder()
                .taskTemplateId(templateId)
                .baseTaskId(node.getBaseTaskId())
                .sort(node.getSort())
                .parallelSort(node.getParallelSort())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private TaskTemplate validateAndGetTemplate(Long templateId, Long orgId) {
        TaskTemplate template = taskTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "任务模板不存在");
        }
        if (!template.getOrgId().equals(orgId)) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "无权操作该任务模板");
        }
        if (template.getStatus() == Status.DISABLED) {
            throw new AppException(ResultCode.OPERATION_FAIL, "任务模板已被禁用");
        }
        return template;
    }
}
