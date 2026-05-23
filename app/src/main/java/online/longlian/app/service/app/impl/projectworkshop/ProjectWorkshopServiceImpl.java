package online.longlian.app.service.app.impl.projectworkshop;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    @Override
    public PageResultBO<WorkshopProjectInfoVO> getMyWorkshopList(WorkshopListParamsBO params) {
        List<ProjectWorkshop> workshops = projectWorkshopMapper.selectList(
                workshopQueryBuilder.buildWorkshopListQuery(params.getUserId()));
        if (workshops.isEmpty()) {
            return new PageResultBO<>(Collections.emptyList(), 0L);
        }

        Map<Long, ProjectWorkshop> workshopMap = workshops.stream()
                .collect(Collectors.toMap(ProjectWorkshop::getProjectId, Function.identity(),
                        (a, b) -> a, LinkedHashMap::new));

        List<Long> projectIds = new ArrayList<>(workshopMap.keySet());
        List<Project> projects = projectMapper.selectList(
                workshopQueryBuilder.buildProjectQuery(params.getOrgId(), projectIds));
        if (projects.isEmpty()) {
            return new PageResultBO<>(Collections.emptyList(), 0L);
        }

        Map<Long, Project> projectMap = projects.stream()
                .collect(Collectors.toMap(Project::getId, Function.identity()));

        List<Project> filteredProjects = workshopProjectHandler.filterProjects(
                projectIds, projectMap, params.getOrgId(), params.getProjectType(),
                params.getKeyword(), params.getUserId(), params.getIsMyCreated());

        long total = filteredProjects.size();
        long fromIndex = (params.getPage().getPageNum() - 1L) * params.getPage().getPageSize();
        if (fromIndex >= total) {
            return new PageResultBO<>(Collections.emptyList(), total);
        }
        List<Project> pageProjects = filteredProjects.subList(
                (int) fromIndex, (int) Math.min(fromIndex + params.getPage().getPageSize(), total));

        return new PageResultBO<>(workshopAssembler.assembleProjectList(pageProjects), total);
    }

    @Override
    public PageResultBO<WorkshopTaskTemplateVO> getWorkshopTaskTemplateList(WorkshopTaskTemplateListParamsBO params) {
        List<TaskTemplate> orgTemplates = taskTemplateMapper.selectList(
                workshopQueryBuilder.buildOrgTemplateQuery(params.getOrgId(), params.getKeyword()));
        List<TaskTemplate> personalTemplates = taskTemplateMapper.selectList(
                workshopQueryBuilder.buildPersonalTemplateQuery(params.getUserId(), params.getKeyword()));

        List<TaskTemplate> allTemplates = new ArrayList<>();
        allTemplates.addAll(orgTemplates);
        allTemplates.addAll(personalTemplates);
        allTemplates.sort(Comparator.comparing(TaskTemplate::getCreatedAt).reversed());

        if (params.getIsMyCreated() != null && params.getIsMyCreated()) {
            allTemplates = allTemplates.stream()
                    .filter(t -> t.getScope() == TaskTemplateScope.PERSONAL
                            && t.getCreatorId().equals(params.getUserId()))
                    .toList();
        }

        long total = allTemplates.size();
        long fromIndex = (params.getPage().getPageNum() - 1L) * params.getPage().getPageSize();
        if (fromIndex >= total) {
            return new PageResultBO<>(Collections.emptyList(), total);
        }
        List<TaskTemplate> pageTemplates = allTemplates.subList(
                (int) fromIndex, (int) Math.min(fromIndex + params.getPage().getPageSize(), total));

        return new PageResultBO<>(
                workshopAssembler.assembleTemplateList(pageTemplates, params.getUserId()), total);
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
