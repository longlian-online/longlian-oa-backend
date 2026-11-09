package online.longlian.app.service.app.impl.project;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import online.longlian.app.common.mybatis.OffsetPage;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ItemMapper;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.mapper.ProjectTypeMapper;
import online.longlian.app.mapper.ProjectWorkshopMapper;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.app.ProjectProgressBO;
import online.longlian.app.pojo.bo.app.ProjectMemberListParamsBO;
import online.longlian.app.pojo.bo.app.ProjectMemberResultBO;
import online.longlian.app.pojo.bo.common.ResourceReadUrlGetResultBO;
import online.longlian.app.service.common.OrganizationMemberDetails;
import online.longlian.app.pojo.bo.app.ProjectAccessParamsBO;
import online.longlian.app.pojo.bo.app.ProjectAccessResultBO;
import online.longlian.app.pojo.bo.app.ProjectCreateParamsBO;
import online.longlian.app.pojo.bo.app.ProjectDeleteParamsBO;
import online.longlian.app.pojo.bo.app.ProjectDetailResultBO;
import online.longlian.app.pojo.bo.app.ProjectListParamsBO;
import online.longlian.app.pojo.bo.app.ProjectListResultBO;
import online.longlian.app.pojo.bo.app.ProjectSearchResultBO;
import online.longlian.app.pojo.bo.app.ProjectUpdateParamsBO;
import online.longlian.app.pojo.bo.app.ProjectWorkshopAddParamsBO;
import online.longlian.app.pojo.bo.app.ProjectWorkshopAddResultBO;
import online.longlian.app.pojo.bo.app.ProjectWorkshopRemoveParamsBO;
import online.longlian.app.pojo.bo.app.ProjectClaimedTaskParamsBO;
import online.longlian.app.pojo.bo.common.ResourceBindParamsBO;
import online.longlian.app.pojo.entity.Item;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.ProjectType;
import online.longlian.app.pojo.entity.ProjectWorkshop;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeCustomFieldResultBO;
import online.longlian.app.pojo.vo.app.ProjectTypeCustomFieldVO;
import online.longlian.app.pojo.vo.app.ProjectTypeInfoVO;
import online.longlian.app.service.app.ProjectService;
import online.longlian.app.service.app.ClaimedTaskLoadService;
import online.longlian.app.service.common.OrganizationMembershipService;
import online.longlian.app.service.orgadmin.ProjectTypeCustomFormService;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.ProjectStatus;
import online.longlian.common.enumeration.Status;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service("appProjectService")
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectMapper projectMapper;
    private final ProjectTypeMapper projectTypeMapper;
    private final ProjectWorkshopMapper projectWorkshopMapper;
    private final ItemMapper itemMapper;
    private final ResourceService resourceService;
    private final ProjectQueryBuilder projectQueryBuilder;
    private final ProjectAssembler projectAssembler;
    private final ProjectProgressHandler projectProgressHandler;
    private final ProjectTypeCustomFormService projectTypeCustomFormService;
    private final ProjectAttachmentHandler projectAttachmentHandler;
    private final OrganizationMembershipService organizationMembershipService;
    private final ClaimedTaskLoadService claimedTaskLoadService;
    private final Clock clock;

    @Override
    public PageResultBO<ProjectMemberResultBO> getProjectMembers(ProjectMemberListParamsBO params) {
        Project project = getVisibleProject(params.getProjectId(), params.getOrgId());
        List<Long> userIds = projectWorkshopMapper.selectList(new LambdaQueryWrapper<ProjectWorkshop>()
                        .eq(ProjectWorkshop::getProjectId, params.getProjectId()))
                .stream().map(ProjectWorkshop::getUserId).distinct().toList();
        // 先过滤已删除的组织成员及用户，再分页，确保 total 和页内条数都基于当前成员。
        List<OrganizationMemberDetails> members = organizationMembershipService.listMemberProfiles(params.getOrgId(), userIds);
        long offset = ((long) params.getPage().getPageNum() - 1) * params.getPage().getPageSize();
        List<OrganizationMemberDetails> page = members.stream().skip(offset).limit(params.getPage().getPageSize()).toList();
        List<Long> avatarIds = page.stream().map(OrganizationMemberDetails::avatarFileId)
                .filter(id -> id != null && id > 0).distinct().toList();
        Map<Long, ResourceReadUrlGetResultBO> avatars = resourceService.getResourceReadUrls(avatarIds);
        List<ProjectMemberResultBO> results = page.stream().map(member -> {
            ResourceReadUrlGetResultBO avatar = member.avatarFileId() == null ? null : avatars.get(member.avatarFileId());
            return new ProjectMemberResultBO(member.userId(), member.nickname(),
                    avatar == null ? null : avatar.getUrl(), project.getCreatorId().equals(member.userId()));
        }).toList();
        return new PageResultBO<>(results, (long) members.size());
    }

    @Override
    public List<ProjectTypeInfoVO> getProjectTypes(Long orgId) {
        List<ProjectType> types = projectTypeMapper.selectList(
                new LambdaQueryWrapper<ProjectType>()
                        .eq(ProjectType::getOrgId, orgId)
                        .eq(ProjectType::getStatus, Status.ENABLED)
                        .orderByDesc(ProjectType::getPriority)
                        .orderByAsc(ProjectType::getId)
        );
        if (types.isEmpty()) {
            return List.of();
        }
        Map<Long, List<ProjectTypeCustomFieldResultBO>> forms = projectTypeCustomFormService.listByTypeIds(
                orgId, types.stream().map(ProjectType::getId).toList());
        return types.stream()
                .map(type -> ProjectTypeInfoVO.builder()
                        .id(type.getId())
                        .name(type.getName())
                        .customFields(forms.getOrDefault(type.getId(), List.of()).stream()
                                .map(field -> ProjectTypeCustomFieldVO.builder()
                                        .presetFieldId(field.getPresetFieldId())
                                        .name(field.getName())
                                        .required(field.getRequired())
                                        .build())
                                .toList())
                        .build())
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
            if (projectType == null) {
                throw new AppException(ResultCode.PARAM_ERROR, "企划类型不存在或已禁用");
            }
            typeId = projectType.getId();
        }

        if (StringUtils.hasText(params.getKeyword())) {
            return searchProjects(params, typeId);
        }

        Page<Project> page = buildWindow(params);
        LambdaQueryWrapper<Project> queryWrapper = projectQueryBuilder.buildListQuery(params, typeId);
        Page<Project> projectPage = projectMapper.selectPage(page, queryWrapper);
        List<Project> projects = projectPage.getRecords();
        if (projects.isEmpty()) {
            return new PageResultBO<>(Collections.emptyList(), projectPage.getTotal());
        }
        return new PageResultBO<>(projectAssembler.assembleList(projects, params.getOrgId()), projectPage.getTotal());
    }

    private PageResultBO<ProjectListResultBO> searchProjects(ProjectListParamsBO params, Long typeId) {
        Page<ProjectSearchResultBO> page = buildWindow(params);
        Page<ProjectSearchResultBO> searchPage = projectMapper.selectSearchPage(
                page, ProjectSearchSupport.buildParams(params, typeId));
        List<ProjectSearchResultBO> records = searchPage.getRecords();
        if (records.isEmpty()) {
            return new PageResultBO<>(Collections.emptyList(), searchPage.getTotal());
        }
        List<ProjectListResultBO> results = projectAssembler.assembleList(
                records.stream().map(ProjectSearchResultBO::getProject).toList(), params.getOrgId());
        for (int i = 0; i < results.size(); i++) {
            ProjectListResultBO result = results.get(i);
            result.setMetadata(ProjectSearchSupport.prioritizeMetadata(
                    result.getMetadata(), records.get(i).getMatchedMetadataPaths()));
        }
        return new PageResultBO<>(results, searchPage.getTotal());
    }

    /** 显式 offset 优先：传入 pageOffset 时以绝对偏移取窗，供各页条数不一致的分页使用 */
    private <T> Page<T> buildWindow(ProjectListParamsBO params) {
        if (params.getPageOffset() != null) {
            return new OffsetPage<>(params.getPage().getPageSize(), params.getPageOffset());
        }
        return new Page<>(params.getPage().getPageNum(), params.getPage().getPageSize());
    }

    @Override
    public ProjectDetailResultBO getProjectDetail(Long projectId, Long userId, Long orgId) {
        Project project = getVisibleProject(projectId, orgId);

        String coverUrl = null;
        if (project.getCoverFileId() != null && project.getCoverFileId() > 0) {
            coverUrl = resourceService.getResourceReadUrl(project.getCoverFileId());
        }
        String thumbnailUrl = null;
        if (project.getThumbnailFileId() != null && project.getThumbnailFileId() > 0) {
            thumbnailUrl = resourceService.getResourceReadUrl(project.getThumbnailFileId());
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
        boolean hasItems = itemMapper.selectCount(new LambdaQueryWrapper<Item>()
                .eq(Item::getProjectId, projectId)) > 0;

        return ProjectDetailResultBO.builder()
                .id(project.getId())
                .title(project.getTitle())
                .alias(project.getAlias())
                .coverUrl(coverUrl)
                .thumbnailUrl(thumbnailUrl)
                .typeName(typeName)
                .metadata(project.getMetadata())
                .description(project.getDescription())
                .status(project.getStatus())
                .progressPercent(progress.getProgressPercent())
                .claimedTaskCount(progress.getClaimedTaskCount())
                .pendingTaskCount(progress.getPendingTaskCount())
                .inWorkshop(inWorkshop)
                .isCreator(isCreator)
                .canManage(isPlanningManager(project.getCreatorId(), userId, orgId))
                .hasItems(hasItems)
                .createdAt(project.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createProject(ProjectCreateParamsBO params) {
        requireEnabledType(params.getTypeId(), params.getOrgId());
        ProjectMetadataValidator.validate(params.getMetadata());
        requireCustomFields(params.getOrgId(), params.getTypeId(), params.getMetadata());

        LocalDateTime now = LocalDateTime.now(clock);
        Project project = Project.builder()
                .orgId(params.getOrgId())
                .typeId(params.getTypeId())
                .title(params.getTitle())
                .alias(params.getAlias())
                .metadata(params.getMetadata())
                .coverFileId(params.getCoverFileId())
                .thumbnailFileId(params.getThumbnailFileId())
                .description(params.getDescription())
                .status(ProjectStatus.IN_PROGRESS)
                .resourceStatus(Status.ENABLED)
                .creatorId(params.getCreatorId())
                .createdAt(now)
                .updatedAt(now)
                .build();
        projectMapper.insert(project);
        resourceService.bindBizResource(ResourceBindParamsBO.builder()
                .resourceId(params.getCoverFileId())
                .bizType("cover")
                .bizId(project.getId())
                .creatorId(params.getCreatorId())
                .orgId(params.getOrgId())
                .build());
        if (params.getThumbnailFileId() != null) {
            resourceService.bindBizResource(ResourceBindParamsBO.builder()
                    .resourceId(params.getThumbnailFileId())
                    .bizType("thumbnail")
                    .bizId(project.getId())
                    .creatorId(params.getCreatorId())
                    .orgId(params.getOrgId())
                    .build());
        }
        projectProgressHandler.addToWorkshop(ProjectWorkshopAddParamsBO.builder()
                .projectId(project.getId())
                .userId(params.getCreatorId())
                .orgId(params.getOrgId())
                .build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProject(ProjectUpdateParamsBO params) {
        Project project = getVisibleProject(params.getProjectId(), params.getOrgId());
        requirePlanningManager(project.getCreatorId(), params.getUserId(), params.getOrgId(), "仅创建者可编辑企划");
        ProjectMetadataValidator.validate(params.getMetadata());
        if (params.getStatus() == ProjectStatus.ARCHIVED) {
            throw new AppException(ResultCode.PARAM_ERROR, "不允许将企划修改为已归档状态");
        }
        if (params.getTypeId() != null) {
            requireEnabledType(params.getTypeId(), params.getOrgId());
        }
        Long typeId = params.getTypeId() != null ? params.getTypeId() : project.getTypeId();
        requireCustomFields(params.getOrgId(), typeId, params.getMetadata());

        projectMapper.update(null,
                new LambdaUpdateWrapper<Project>()
                        .eq(Project::getId, params.getProjectId())
                        .set(Project::getTitle, params.getTitle())
                        .set(Project::getAlias, params.getAlias())
                        .set(Project::getMetadata, params.getMetadata())
                        .set(Project::getDescription, params.getDescription())
                        .set(params.getTypeId() != null, Project::getTypeId, params.getTypeId())
                        .set(params.getCoverFileId() != null, Project::getCoverFileId, params.getCoverFileId())
                        .set(params.getThumbnailFileId() != null, Project::getThumbnailFileId, params.getThumbnailFileId())
                        .set(params.getStatus() != null, Project::getStatus, params.getStatus())
                        .set(Project::getUpdatedAt, LocalDateTime.now(clock))
        );
        if (params.getCoverFileId() != null && !params.getCoverFileId().equals(project.getCoverFileId())) {
            resourceService.bindBizResource(ResourceBindParamsBO.builder()
                    .resourceId(params.getCoverFileId())
                    .bizType("cover")
                    .bizId(params.getProjectId())
                    .creatorId(params.getUserId())
                    .orgId(params.getOrgId())
                    .build());
        }
        if (params.getThumbnailFileId() != null
                && !params.getThumbnailFileId().equals(project.getThumbnailFileId())) {
            resourceService.bindBizResource(ResourceBindParamsBO.builder()
                    .resourceId(params.getThumbnailFileId())
                    .bizType("thumbnail")
                    .bizId(params.getProjectId())
                    .creatorId(params.getUserId())
                    .orgId(params.getOrgId())
                    .build());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectWorkshopAddResultBO addToWorkshop(ProjectWorkshopAddParamsBO params) {
        getVisibleProject(params.getProjectId(), params.getOrgId());
        return projectProgressHandler.addToWorkshop(params);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFromWorkshop(ProjectWorkshopRemoveParamsBO params) {
        Project project = getVisibleProject(params.getProjectId(), params.getOrgId());
        if (project.getCreatorId().equals(params.getUserId())) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "企划负责人不能退出企划");
        }
        // 先锁定成员关联，再当前读检查责任；接取/分配在同一事务中确认关联，避免退出漏掉并发接取。
        projectWorkshopMapper.update(null,
                new LambdaUpdateWrapper<ProjectWorkshop>()
                        .eq(ProjectWorkshop::getProjectId, params.getProjectId())
                        .eq(ProjectWorkshop::getUserId, params.getUserId())
                        .set(ProjectWorkshop::getDeletedAt, LocalDateTime.now(clock))
        );
        if (claimedTaskLoadService.hasClaimedTasksInProject(ProjectClaimedTaskParamsBO.builder()
                .projectId(params.getProjectId()).assigneeId(params.getUserId()).build())) {
            throw new AppException(ResultCode.OPERATION_FAIL, "还有未完成的任务，请先提交或放弃后再退出企划");
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProject(ProjectDeleteParamsBO params) {
        Project project = getVisibleProject(params.getProjectId(), params.getOrgId());
        requirePlanningManager(project.getCreatorId(), params.getUserId(), params.getOrgId(), "仅企划负责人可删除企划");
        if (itemMapper.selectCount(new LambdaQueryWrapper<Item>()
                .eq(Item::getProjectId, params.getProjectId())) > 0) {
            throw new AppException(ResultCode.PARAM_ERROR, "企划已存在项目，请提交删除申请");
        }
        projectAttachmentHandler.deleteProjectAttachments(params.getProjectId());
        projectMapper.deleteById(params.getProjectId());
    }

    @Override
    public String requireCreatorProjectTitle(Long projectId, Long userId, Long orgId) {
        Project project = getVisibleProject(projectId, orgId);
        requirePlanningManager(project.getCreatorId(), userId, orgId, "仅企划负责人可删除企划");
        return project.getTitle();
    }

    @Override
    public boolean isPlanningManager(Long creatorId, Long userId, Long orgId) {
        if (creatorId != null && creatorId.equals(userId)) {
            return true;
        }
        return organizationMembershipService.isEnabledAdministrator(userId, orgId);
    }

    private void requirePlanningManager(Long creatorId, Long userId, Long orgId, String deniedMessage) {
        if (!isPlanningManager(creatorId, userId, orgId)) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, deniedMessage);
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public ProjectAccessResultBO lockEnabledProject(ProjectAccessParamsBO params) {
        Project project = projectMapper.selectOne(new LambdaQueryWrapper<Project>()
                .eq(Project::getId, params.getProjectId())
                .last("FOR UPDATE"));
        if (project == null || !project.getOrgId().equals(params.getOrgId())
                || project.getResourceStatus() != Status.ENABLED) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "企划不存在");
        }
        return new ProjectAccessResultBO(project.getCreatorId());
    }

    @Override
    public ProjectAccessResultBO requireVisibleProject(ProjectAccessParamsBO params) {
        return new ProjectAccessResultBO(getVisibleProject(params.getProjectId(), params.getOrgId()).getCreatorId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProjectForReview(Long projectId, Long orgId) {
        Project project = getVisibleProject(projectId, orgId);
        projectAttachmentHandler.deleteProjectAttachments(projectId);
        projectMapper.deleteById(project.getId());
    }

    private Project getVisibleProject(Long projectId, Long orgId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null || !project.getOrgId().equals(orgId)
                || project.getResourceStatus() != Status.ENABLED) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "企划不存在");
        }
        return project;
    }

    private void requireCustomFields(Long orgId, Long typeId, String metadata) {
        ProjectCustomFieldRequirements.requireSatisfied(
                metadata, projectTypeCustomFormService.requiredFieldNames(orgId, typeId));
    }

    private void requireEnabledType(Long typeId, Long orgId) {
        ProjectType projectType = projectTypeMapper.selectById(typeId);
        if (projectType == null || !projectType.getOrgId().equals(orgId)
                || projectType.getStatus() != Status.ENABLED) {
            throw new AppException(ResultCode.PARAM_ERROR, "企划类型不存在或已禁用");
        }
    }
}
