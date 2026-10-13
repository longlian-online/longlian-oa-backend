package online.longlian.app.service.app.impl.projectworkshop;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.enumeration.WorkshopSortBy;
import online.longlian.app.pojo.bo.app.WorkshopListParamsBO;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.TaskTemplate;
import online.longlian.common.enumeration.Status;
import online.longlian.common.enumeration.TaskTemplateScope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class WorkshopQueryBuilder {

    public LambdaQueryWrapper<Project> buildWorkshopProjectQuery(WorkshopListParamsBO params, Long typeId) {
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<Project>()
                .eq(Project::getOrgId, params.getOrgId())
                .eq(Project::getResourceStatus, Status.ENABLED)
                .exists("""
                        SELECT 1 FROM project_workshop pw
                        WHERE pw.project_id = project.id AND pw.user_id = {0} AND pw.deleted_at IS NULL
                        """, params.getUserId());
        if (StringUtils.hasText(params.getKeyword())) {
            wrapper.like(Project::getTitle, params.getKeyword().trim());
        }
        if (typeId != null) {
            wrapper.eq(Project::getTypeId, typeId);
        }
        if (Boolean.TRUE.equals(params.getIsMyCreated())) {
            wrapper.eq(Project::getCreatorId, params.getUserId());
        }
        if (params.getSortBy() == WorkshopSortBy.CREATED_ASC) {
            wrapper.orderByAsc(Project::getCreatedAt, Project::getId);
        } else if (params.getSortBy() == WorkshopSortBy.CREATED_DESC) {
            wrapper.orderByDesc(Project::getCreatedAt, Project::getId);
        } else {
            // 提交时间是聚合值而非实体字段；固定 SQL 不接受任何用户输入。
            wrapper.last("""
                    ORDER BY (
                        SELECT MAX(s.created_at) FROM task_submission s
                        WHERE s.project_id = project.id AND s.deleted_at IS NULL
                    ) DESC, project.created_at DESC, project.id DESC
                    """);
        }
        return wrapper;
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
