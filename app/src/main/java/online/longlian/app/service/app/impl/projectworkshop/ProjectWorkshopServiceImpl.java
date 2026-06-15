package online.longlian.app.service.app.impl.projectworkshop;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.mapper.ProjectWorkshopMapper;
import online.longlian.app.mapper.TaskTemplateMapper;
import online.longlian.app.mapper.TaskTemplateNodeMapper;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.app.WorkshopListParamsBO;
import online.longlian.app.pojo.bo.app.WorkshopTaskTemplateCreateParamsBO;
import online.longlian.app.pojo.bo.app.WorkshopTaskTemplateListParamsBO;
import online.longlian.app.pojo.bo.app.WorkshopTaskTemplateNodeCreateParamsBO;
import online.longlian.app.pojo.bo.app.WorkshopTaskTemplateUpdateParamsBO;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.ProjectWorkshop;
import online.longlian.app.pojo.entity.TaskTemplate;
import online.longlian.app.pojo.entity.TaskTemplateNode;
import online.longlian.app.pojo.vo.app.WorkshopProjectInfoVO;
import online.longlian.app.pojo.vo.app.WorkshopTaskTemplateVO;
import online.longlian.app.service.app.ProjectWorkshopService;
import online.longlian.common.enumeration.Status;
import online.longlian.common.enumeration.TaskTemplateScope;
import online.longlian.common.service.DistributedLockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ProjectWorkshopServiceImpl extends ServiceImpl<ProjectWorkshopMapper, ProjectWorkshop> implements ProjectWorkshopService {

    private final ProjectWorkshopMapper projectWorkshopMapper;
    private final ProjectMapper projectMapper;
    private final TaskTemplateMapper taskTemplateMapper;
    private final TaskTemplateNodeMapper taskTemplateNodeMapper;
    private final Clock clock;
    private final WorkshopQueryBuilder workshopQueryBuilder;
    private final WorkshopAssembler workshopAssembler;
    private final WorkshopProjectHandler workshopProjectHandler;
    private final DistributedLockService lockService;

    @Override
    public PageResultBO<WorkshopProjectInfoVO> getMyWorkshopList(WorkshopListParamsBO params) {
        List<ProjectWorkshop> workshops = projectWorkshopMapper.selectList(
                workshopQueryBuilder.buildWorkshopListQuery(params.getUserId()));
        if (workshops.isEmpty()) {
            return new PageResultBO<>(Collections.emptyList(), 0L);
        }

        List<Long> projectIds = workshops.stream()
                .map(ProjectWorkshop::getProjectId)
                .distinct()
                .toList();

        Long typeId = workshopProjectHandler.resolveTypeId(params.getOrgId(), params.getProjectType());

        Page<Project> page = new Page<>(params.getPage().getPageNum(), params.getPage().getPageSize());
        LambdaQueryWrapper<Project> queryWrapper = workshopQueryBuilder.buildFilteredProjectQuery(
                params.getOrgId(), projectIds, params.getKeyword(), typeId,
                params.getIsMyCreated(), params.getUserId());
        Page<Project> projectPage = projectMapper.selectPage(page, queryWrapper);

        if (projectPage.getRecords().isEmpty()) {
            return new PageResultBO<>(Collections.emptyList(), projectPage.getTotal());
        }

        return new PageResultBO<>(
                workshopAssembler.assembleProjectList(projectPage.getRecords()),
                projectPage.getTotal());
    }

    @Override
    public PageResultBO<WorkshopTaskTemplateVO> getWorkshopTaskTemplateList(WorkshopTaskTemplateListParamsBO params) {
        Page<TaskTemplate> page = new Page<>(params.getPage().getPageNum(), params.getPage().getPageSize());
        LambdaQueryWrapper<TaskTemplate> queryWrapper = workshopQueryBuilder.buildCombinedTemplateQuery(
                params.getOrgId(), params.getUserId(), params.getKeyword(), params.getIsMyCreated());
        Page<TaskTemplate> templatePage = taskTemplateMapper.selectPage(page, queryWrapper);

        if (templatePage.getRecords().isEmpty()) {
            return new PageResultBO<>(Collections.emptyList(), templatePage.getTotal());
        }

        return new PageResultBO<>(
                workshopAssembler.assembleTemplateList(templatePage.getRecords(), params.getUserId()),
                templatePage.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createWorkshopTaskTemplate(WorkshopTaskTemplateCreateParamsBO params) {
        LocalDateTime now = LocalDateTime.now(clock);
        TaskTemplate template = TaskTemplate.builder()
                .orgId(params.getOrgId())
                .name(params.getName())
                .description(params.getDescription())
                .status(Status.ENABLED)
                .scope(TaskTemplateScope.PERSONAL)
                .refCount(0)
                .creatorId(params.getCreatorId())
                .createdAt(now)
                .updatedAt(now)
                .build();
        taskTemplateMapper.insert(template);

        for (WorkshopTaskTemplateNodeCreateParamsBO node : params.getNodes()) {
            TaskTemplateNode taskTemplateNode = buildNode(template.getId(), node, now);
            taskTemplateNodeMapper.insert(taskTemplateNode);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWorkshopTaskTemplate(WorkshopTaskTemplateUpdateParamsBO params) {
        String lockKey = "template:edit:" + params.getTemplateId();
        try (var lock = lockService.tryAcquire(lockKey, 0, 30, TimeUnit.SECONDS)) {
            if (lock == null) {
                throw new AppException(ResultCode.OPERATION_FAIL, "操作过于频繁，请稍后再试");
            }
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
                            .isNull(TaskTemplateNode::getDeletedAt)
                            .set(TaskTemplateNode::getDeletedAt, now));

            for (WorkshopTaskTemplateNodeCreateParamsBO node : params.getNodes()) {
                TaskTemplateNode taskTemplateNode = buildNode(params.getTemplateId(), node, now);
                taskTemplateNodeMapper.insert(taskTemplateNode);
            }
        }
    }

    private TaskTemplateNode buildNode(Long templateId, WorkshopTaskTemplateNodeCreateParamsBO node, LocalDateTime now) {
        return TaskTemplateNode.builder()
                .taskTemplateId(templateId)
                .baseTaskId(node.getBaseTaskId())
                .sort(node.getSort())
                .parallelSort(node.getParallelSort())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
