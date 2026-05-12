package online.longlian.app.service.orgadmin.impl.project;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.pojo.bo.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectAdminListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectAdminListResultBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectChangeStatusParamsBO;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.service.orgadmin.ProjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    private final ProjectMapper projectMapper;
    private final Clock clock;
    private final ProjectQueryBuilder projectQueryBuilder;
    private final ProjectAssembler projectAssembler;

    @Override
    public PageResultBO<ProjectAdminListResultBO> getAdminProjectList(ProjectAdminListParamsBO params) {
        Page<Project> page = new Page<>(params.getPage().getPageNum(), params.getPage().getPageSize());
        LambdaQueryWrapper<Project> queryWrapper = projectQueryBuilder.buildAdminListQuery(params);
        Page<Project> projectPage = projectMapper.selectPage(page, queryWrapper);
        List<Project> projects = projectPage.getRecords();
        if (projects.isEmpty()) {
            return new PageResultBO<>(Collections.emptyList(), projectPage.getTotal());
        }
        return new PageResultBO<>(projectAssembler.assembleAdminList(projects), projectPage.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeProjectStatus(ProjectChangeStatusParamsBO params) {
        Project project = projectMapper.selectById(params.getProjectId());
        if (project == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "企划不存在");
        }
        if (!project.getOrgId().equals(params.getOrgId())) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "无权操作该企划");
        }
        projectMapper.update(null,
                new LambdaUpdateWrapper<Project>()
                        .eq(Project::getId, params.getProjectId())
                        .set(Project::getStatus, params.getStatus())
                        .set(Project::getUpdatedAt, LocalDateTime.now(clock))
        );
    }
}
