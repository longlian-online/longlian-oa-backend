package online.longlian.app.service.app.impl.projectworkshop;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ProjectTypeMapper;
import online.longlian.app.mapper.TaskTemplateMapper;
import online.longlian.app.mapper.TaskTemplateNodeMapper;
import online.longlian.app.pojo.bo.app.WorkshopTaskTemplateNodeCreateParamsBO;
import online.longlian.app.pojo.bo.app.WorkshopTaskTemplateUpdateParamsBO;
import online.longlian.app.pojo.entity.ProjectType;
import online.longlian.app.pojo.entity.TaskTemplate;
import online.longlian.app.pojo.entity.TaskTemplateNode;
import online.longlian.common.enumeration.Status;
import online.longlian.common.enumeration.TaskTemplateScope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class WorkshopProjectHandler {

    private final ProjectTypeMapper projectTypeMapper;
    private final TaskTemplateMapper taskTemplateMapper;
    private final TaskTemplateNodeMapper taskTemplateNodeMapper;
    private final Clock clock;

    public Long resolveTypeId(Long orgId, String projectTypeName) {
        if (!StringUtils.hasText(projectTypeName)) {
            return null;
        }
        ProjectType projectType = projectTypeMapper.selectOne(
                new LambdaQueryWrapper<ProjectType>()
                        .eq(ProjectType::getOrgId, orgId)
                        .eq(ProjectType::getName, projectTypeName.trim())
                        .eq(ProjectType::getStatus, Status.ENABLED)
                        .last("LIMIT 1"));
        return projectType != null ? projectType.getId() : null;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateWorkshopTaskTemplate(WorkshopTaskTemplateUpdateParamsBO params) {
        TaskTemplate template = taskTemplateMapper.selectById(params.getTemplateId());
        if (template == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "任务模板不存在");
        }
        if (template.getScope() != TaskTemplateScope.PERSONAL) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "仅可编辑个人模板");
        }
        if (!template.getCreatorId().equals(params.getUserId())) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "仅创建者可编辑该模板");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        taskTemplateMapper.update(null,
                new LambdaUpdateWrapper<TaskTemplate>()
                        .eq(TaskTemplate::getId, params.getTemplateId())
                        .set(TaskTemplate::getName, params.getName())
                        .set(TaskTemplate::getDescription, params.getDescription())
                        .set(TaskTemplate::getUpdatedAt, now));

        taskTemplateNodeMapper.update(null,
                new LambdaUpdateWrapper<TaskTemplateNode>()
                        .eq(TaskTemplateNode::getTaskTemplateId, params.getTemplateId())
                        .set(TaskTemplateNode::getDeletedAt, now));

        insertNewNodes(params.getTemplateId(), params.getNodes(), now);
    }

    private void insertNewNodes(Long templateId, List<WorkshopTaskTemplateNodeCreateParamsBO> nodes, LocalDateTime now) {
        for (WorkshopTaskTemplateNodeCreateParamsBO node : nodes) {
            taskTemplateNodeMapper.insert(
                    TaskTemplateNode.builder()
                            .taskTemplateId(templateId)
                            .baseTaskId(node.getBaseTaskId())
                            .sort(node.getSort())
                            .parallelSort(node.getParallelSort())
                            .createdAt(now)
                            .updatedAt(now)
                            .build());
        }
    }
}
