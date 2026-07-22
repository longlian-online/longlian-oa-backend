package online.longlian.app.service.app.impl.project;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.mapper.ProjectTypeMapper;
import online.longlian.app.mapper.ProjectWorkshopMapper;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.app.ProjectProgressBO;
import online.longlian.app.pojo.bo.app.ProjectCreateParamsBO;
import online.longlian.app.pojo.bo.app.ProjectDetailResultBO;
import online.longlian.app.pojo.bo.app.ProjectListParamsBO;
import online.longlian.app.pojo.bo.app.ProjectListResultBO;
import online.longlian.app.pojo.bo.app.ProjectUpdateParamsBO;
import online.longlian.app.pojo.bo.app.ProjectWorkshopAddParamsBO;
import online.longlian.app.pojo.bo.app.ProjectWorkshopRemoveParamsBO;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.ProjectType;
import online.longlian.app.pojo.entity.ProjectWorkshop;
import online.longlian.app.pojo.vo.app.ProjectTypeInfoVO;
import online.longlian.app.service.app.ProjectService;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.ProjectStatus;
import online.longlian.common.enumeration.Status;
import online.longlian.app.service.common.LockService;
import online.longlian.common.service.DistributedLockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service("appProjectService")
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectMapper projectMapper;
    private final ProjectTypeMapper projectTypeMapper;
    private final ProjectWorkshopMapper projectWorkshopMapper;
    private final ResourceService resourceService;
    private final ProjectQueryBuilder projectQueryBuilder;
    private final ProjectAssembler projectAssembler;
    private final ProjectProgressHandler projectProgressHandler;
    private final LockService lockService;
    private final Clock clock;

    @Override
    public List<ProjectTypeInfoVO> getProjectTypes(Long orgId) {
        List<ProjectType> types = projectTypeMapper.selectList(
                new LambdaQueryWrapper<ProjectType>()
                        .eq(ProjectType::getOrgId, orgId)
                        .eq(ProjectType::getStatus, Status.ENABLED)
        );
        return types.stream()
                .map(t -> new ProjectTypeInfoVO(t.getId(), t.getName()))
                .toList();
    }

    @Override
    public PageResultBO<ProjectListResultBO> getProjectList(ProjectListParamsBO params) {
        Long typeId = null;
        if (StringUtils.hasText(params.getProjectType())) {
            ProjectType projectType = projectTypeMapper.selectOne(
                    new LambdaQueryWrapper<ProjectType>()
                            .eq(ProjectType::getOrgId, params.getOrgId())
                            .eq(ProjectType::getName, params.getProjectType().trim())
                            .eq(ProjectType::getStatus, Status.ENABLED)
                            .last("LIMIT 1")
            );
            typeId = projectType != null ? projectType.getId() : null;
        }

        Page<Project> page = new Page<>(params.getPage().getPageNum(), params.getPage().getPageSize());
        LambdaQueryWrapper<Project> queryWrapper = projectQueryBuilder.buildListQuery(params, typeId);
        Page<Project> projectPage = projectMapper.selectPage(page, queryWrapper);
        List<Project> projects = projectPage.getRecords();
        if (projects.isEmpty()) {
            return new PageResultBO<>(Collections.emptyList(), projectPage.getTotal());
        }
        return new PageResultBO<>(projectAssembler.assembleList(projects), projectPage.getTotal());
    }

    @Override
    public ProjectDetailResultBO getProjectDetail(Long projectId, Long userId, Long orgId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null || !project.getOrgId().equals(orgId)) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "企划不存在");
        }

        String coverUrl = null;
        if (project.getCoverFileId() != null && project.getCoverFileId() > 0) {
            coverUrl = resourceService.getResourceReadUrl(project.getCoverFileId());
        }

        String typeName = null;
        if (project.getTypeId() != null) {
            ProjectType projectType = projectTypeMapper.selectById(project.getTypeId());
            typeName = projectType != null ? projectType.getName() : null;
        }

        boolean isCreator = project.getCreatorId().equals(userId);

        boolean inWorkshop = projectWorkshopMapper.selectCount(
                new LambdaQueryWrapper<ProjectWorkshop>()
                        .eq(ProjectWorkshop::getProjectId, projectId)
                        .eq(ProjectWorkshop::getUserId, userId)
        ) > 0;

        ProjectProgressBO progress = projectProgressHandler.computeProgress(projectId);

        return ProjectDetailResultBO.builder()
                .id(project.getId())
                .title(project.getTitle())
                .alias(project.getAlias())
                .coverUrl(coverUrl)
                .typeName(typeName)
                .metadata(project.getMetadata())
                .description(project.getDescription())
                .status(project.getStatus())
                .progressPercent(progress.getProgressPercent())
                .claimedTaskCount(progress.getClaimedTaskCount())
                .pendingTaskCount(progress.getPendingTaskCount())
                .inWorkshop(inWorkshop)
                .isCreator(isCreator)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createProject(ProjectCreateParamsBO params) {
        ProjectType projectType = projectTypeMapper.selectById(params.getTypeId());
        if (projectType == null || !projectType.getOrgId().equals(params.getOrgId())
                || projectType.getStatus() != Status.ENABLED) {
            throw new AppException(ResultCode.PARAM_ERROR, "企划类型不存在或已禁用");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Project project = Project.builder()
                .orgId(params.getOrgId())
                .typeId(params.getTypeId())
                .title(params.getTitle())
                .alias(params.getAlias())
                .metadata(params.getMetadata())
                .coverFileId(params.getCoverFileId())
                .description(params.getDescription())
                .status(ProjectStatus.IN_PROGRESS)
                .creatorId(params.getCreatorId())
                .createdAt(now)
                .updatedAt(now)
                .build();
        projectMapper.insert(project);
        resourceService.bindBizId(params.getCoverFileId(), project.getId(), params.getCreatorId(), params.getOrgId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProject(ProjectUpdateParamsBO params) {
        Project project = projectMapper.selectById(params.getProjectId());
        if (project == null || !project.getOrgId().equals(params.getOrgId())) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "企划不存在");
        }
        if (!project.getCreatorId().equals(params.getUserId())) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "仅创建者可编辑企划");
        }

        projectMapper.update(null,
                new LambdaUpdateWrapper<Project>()
                        .eq(Project::getId, params.getProjectId())
                        .set(Project::getTitle, params.getTitle())
                        .set(Project::getAlias, params.getAlias())
                        .set(Project::getMetadata, params.getMetadata())
                        .set(Project::getDescription, params.getDescription())
                        .set(Project::getCoverFileId, params.getCoverFileId())
                        .set(Project::getUpdatedAt, LocalDateTime.now(clock))
        );
        resourceService.bindBizId(params.getCoverFileId(), params.getProjectId(), params.getUserId(), params.getOrgId());
    }

    @Override
    public void addToWorkshop(ProjectWorkshopAddParamsBO params) {
        Project project = projectMapper.selectById(params.getProjectId());
        if (project == null || !project.getOrgId().equals(params.getOrgId())) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "企划不存在");
        }

        String lockKey = "workshop:add:" + params.getProjectId() + ":" + params.getUserId();
        try (DistributedLockService.Lock lock = lockService.tryAcquireOrThrow(lockKey, 0, 5, TimeUnit.SECONDS)) {
            projectProgressHandler.addToWorkshop(params);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFromWorkshop(ProjectWorkshopRemoveParamsBO params) {
        Project project = projectMapper.selectById(params.getProjectId());
        if (project == null || !project.getOrgId().equals(params.getOrgId())) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "企划不存在");
        }

        projectWorkshopMapper.update(null,
                new LambdaUpdateWrapper<ProjectWorkshop>()
                        .eq(ProjectWorkshop::getProjectId, params.getProjectId())
                        .eq(ProjectWorkshop::getUserId, params.getUserId())
                        .isNull(ProjectWorkshop::getDeletedAt)
                        .set(ProjectWorkshop::getDeletedAt, LocalDateTime.now(clock))
        );
    }
}
