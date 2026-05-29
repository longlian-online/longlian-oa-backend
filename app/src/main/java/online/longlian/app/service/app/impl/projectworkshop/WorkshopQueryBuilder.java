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

    public LambdaQueryWrapper<Project> buildFilteredProjectQuery(Long orgId, List<Long> projectIds,
            String keyword, Long typeId, Boolean isMyCreated, Long userId) {
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<Project>()
                .in(Project::getId, projectIds)
                .eq(Project::getOrgId, orgId);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Project::getTitle, keyword.trim());
        }
        if (typeId != null) {
            wrapper.eq(Project::getTypeId, typeId);
        }
        if (isMyCreated != null && isMyCreated) {
            wrapper.eq(Project::getCreatorId, userId);
        }
        wrapper.orderByDesc(Project::getId);
        return wrapper;
    }

    public LambdaQueryWrapper<ProjectWorkshop> buildWorkshopListQuery(Long userId) {
        return new LambdaQueryWrapper<ProjectWorkshop>()
                .eq(ProjectWorkshop::getUserId, userId)
                .orderByDesc(ProjectWorkshop::getCreatedAt);
    }

    public LambdaQueryWrapper<TaskTemplate> buildCombinedTemplateQuery(Long orgId, Long userId,
            String keyword, Boolean isMyCreated) {
        LambdaQueryWrapper<TaskTemplate> wrapper = new LambdaQueryWrapper<TaskTemplate>()
                .eq(TaskTemplate::getStatus, Status.ENABLED)
                .and(w -> w.and(org -> org.eq(TaskTemplate::getOrgId, orgId)
                                .eq(TaskTemplate::getScope, TaskTemplateScope.ORGANIZATION))
                        .or(personal -> personal.eq(TaskTemplate::getCreatorId, userId)
                                .eq(TaskTemplate::getScope, TaskTemplateScope.PERSONAL)));
        if (StringUtils.hasText(keyword)) {
            wrapper.like(TaskTemplate::getName, keyword.trim());
        }
        if (isMyCreated != null && isMyCreated) {
            wrapper.eq(TaskTemplate::getScope, TaskTemplateScope.PERSONAL)
                   .eq(TaskTemplate::getCreatorId, userId);
        }
        wrapper.orderByDesc(TaskTemplate::getCreatedAt);
        return wrapper;
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
