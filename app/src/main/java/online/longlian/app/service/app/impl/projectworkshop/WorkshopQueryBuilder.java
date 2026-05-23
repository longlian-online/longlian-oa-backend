package online.longlian.app.service.app.impl.projectworkshop;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.ProjectWorkshop;
import online.longlian.app.pojo.entity.TaskTemplate;
import online.longlian.common.enumeration.Status;
import online.longlian.common.enumeration.TaskTemplateScope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
@RequiredArgsConstructor
public class WorkshopQueryBuilder {

    public LambdaQueryWrapper<Project> buildProjectQuery(Long orgId, List<Long> projectIds) {
        return new LambdaQueryWrapper<Project>()
                .in(Project::getId, projectIds)
                .eq(Project::getOrgId, orgId);
    }

    public LambdaQueryWrapper<ProjectWorkshop> buildWorkshopListQuery(Long userId) {
        return new LambdaQueryWrapper<ProjectWorkshop>()
                .eq(ProjectWorkshop::getUserId, userId)
                .orderByDesc(ProjectWorkshop::getCreatedAt);
    }

    public LambdaQueryWrapper<TaskTemplate> buildOrgTemplateQuery(Long orgId, String keyword) {
        LambdaQueryWrapper<TaskTemplate> queryWrapper = new LambdaQueryWrapper<TaskTemplate>()
                .eq(TaskTemplate::getOrgId, orgId)
                .eq(TaskTemplate::getScope, TaskTemplateScope.ORGANIZATION)
                .eq(TaskTemplate::getStatus, Status.ENABLED);
        if (StringUtils.hasText(keyword)) {
            queryWrapper.like(TaskTemplate::getName, keyword.trim());
        }
        queryWrapper.orderByDesc(TaskTemplate::getCreatedAt);
        return queryWrapper;
    }

    public LambdaQueryWrapper<TaskTemplate> buildPersonalTemplateQuery(Long userId, String keyword) {
        LambdaQueryWrapper<TaskTemplate> queryWrapper = new LambdaQueryWrapper<TaskTemplate>()
                .eq(TaskTemplate::getCreatorId, userId)
                .eq(TaskTemplate::getScope, TaskTemplateScope.PERSONAL)
                .eq(TaskTemplate::getStatus, Status.ENABLED);
        if (StringUtils.hasText(keyword)) {
            queryWrapper.like(TaskTemplate::getName, keyword.trim());
        }
        queryWrapper.orderByDesc(TaskTemplate::getCreatedAt);
        return queryWrapper;
    }
}
