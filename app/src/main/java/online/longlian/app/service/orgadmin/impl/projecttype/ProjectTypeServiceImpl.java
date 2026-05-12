package online.longlian.app.service.orgadmin.impl.projecttype;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ProjectTypeMapper;
import online.longlian.app.pojo.bo.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeChangeStatusParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeCreateParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeListResultBO;
import online.longlian.app.pojo.entity.ProjectType;
import online.longlian.app.service.orgadmin.ProjectTypeService;
import online.longlian.generator.enumeration.Status;
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
        LocalDateTime now = LocalDateTime.now(clock);
        ProjectType projectType = ProjectType.builder()
                .orgId(params.getOrgId())
                .name(params.getName())
                .status(Status.ENABLED)
                .creatorId(params.getCreatorId())
                .createdAt(now)
                .updatedAt(now)
                .build();
        projectTypeMapper.insert(projectType);
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
                        .set(ProjectType::getStatus, params.getStatus())
                        .set(ProjectType::getUpdatedAt, LocalDateTime.now(clock))
        );
    }
}
