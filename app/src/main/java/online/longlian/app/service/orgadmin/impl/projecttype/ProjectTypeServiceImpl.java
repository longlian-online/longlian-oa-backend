package online.longlian.app.service.orgadmin.impl.projecttype;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ProjectTypeMapper;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeChangeStatusParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeCreateParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeDeleteParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeListResultBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeUpdateParamsBO;
import online.longlian.app.pojo.entity.ProjectType;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.service.orgadmin.ProjectTypeService;
import online.longlian.common.enumeration.Status;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectTypeServiceImpl implements ProjectTypeService {

    private final ProjectTypeMapper projectTypeMapper;
    private final ProjectMapper projectMapper;
    private final Clock clock;
    private final ProjectTypeQueryBuilder projectTypeQueryBuilder;
    private final ProjectTypeAssembler projectTypeAssembler;

    @Override
    public PageResultBO<ProjectTypeListResultBO> listProjectTypes(ProjectTypeListParamsBO params) {
        Page<ProjectType> page = new Page<>(params.getPage().getPageNum(), params.getPage().getPageSize());
        LambdaQueryWrapper<ProjectType> queryWrapper = projectTypeQueryBuilder.buildListQuery(params);
        Page<ProjectType> projectTypePage = projectTypeMapper.selectPage(page, queryWrapper);
        List<ProjectType> projectTypes = projectTypePage.getRecords();
        if (projectTypes.isEmpty()) {
            return new PageResultBO<>(Collections.emptyList(), projectTypePage.getTotal());
        }
        return new PageResultBO<>(projectTypeAssembler.assembleList(projectTypes), projectTypePage.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createProjectType(ProjectTypeCreateParamsBO params) {
        String name = params.getName().trim();
        if (projectTypeMapper.selectCount(new LambdaQueryWrapper<ProjectType>()
                .eq(ProjectType::getOrgId, params.getOrgId()).eq(ProjectType::getName, name)) > 0) {
            throw new AppException(ResultCode.PARAM_ERROR, "该组织下已存在同名企划类型");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        ProjectType projectType = ProjectType.builder()
                .orgId(params.getOrgId())
                .name(name)
                .status(Status.ENABLED)
                .creatorId(params.getCreatorId())
                .createdAt(now)
                .updatedAt(now)
                .build();
        projectTypeMapper.insert(projectType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProjectType(ProjectTypeUpdateParamsBO params) {
        ProjectType type = requireOwnedType(params.getTypeId(), params.getOrgId());
        String name = params.getName().trim();
        Long duplicate = projectTypeMapper.selectCount(new LambdaQueryWrapper<ProjectType>()
                .eq(ProjectType::getOrgId, params.getOrgId())
                .eq(ProjectType::getName, name)
                .ne(ProjectType::getId, type.getId()));
        if (duplicate > 0) {
            throw new AppException(ResultCode.PARAM_ERROR, "该组织下已存在同名企划类型");
        }
        projectTypeMapper.update(null, new LambdaUpdateWrapper<ProjectType>()
                .eq(ProjectType::getId, type.getId()).set(ProjectType::getName, name)
                .set(ProjectType::getUpdatedAt, LocalDateTime.now(clock)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProjectType(ProjectTypeDeleteParamsBO params) {
        Long typeId = params.getTypeId();
        Long orgId = params.getOrgId();
        requireOwnedType(typeId, orgId);
        Long used = projectMapper.selectCount(new LambdaQueryWrapper<Project>()
                .eq(Project::getOrgId, orgId)
                .eq(Project::getTypeId, typeId));
        if (used > 0) {
            throw new AppException(ResultCode.PARAM_ERROR, "该类型已被 " + used + " 个企划使用，请改为禁用");
        }
        projectTypeMapper.deleteById(typeId);
    }

    private ProjectType requireOwnedType(Long typeId, Long orgId) {
        ProjectType type = projectTypeMapper.selectById(typeId);
        if (type == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "企划类型不存在");
        }
        if (!type.getOrgId().equals(orgId)) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "无权操作该企划类型");
        }
        return type;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeProjectTypeStatus(ProjectTypeChangeStatusParamsBO params) {
        ProjectType projectType = projectTypeMapper.selectById(params.getTypeId());
        if (projectType == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "企划类型不存在");
        }
        if (!projectType.getOrgId().equals(params.getOrgId())) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "无权操作该企划类型");
        }
        projectTypeMapper.update(null,
                new LambdaUpdateWrapper<ProjectType>()
                        .eq(ProjectType::getId, params.getTypeId())
                        .ne(ProjectType::getStatus, params.getStatus())
                        .set(ProjectType::getStatus, params.getStatus())
                        .set(ProjectType::getUpdatedAt, LocalDateTime.now(clock))
        );
    }
}
