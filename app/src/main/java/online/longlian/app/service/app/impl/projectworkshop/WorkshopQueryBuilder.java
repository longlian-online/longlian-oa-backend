package online.longlian.app.service.app.impl.projectworkshop;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.pojo.entity.TaskTemplate;
import online.longlian.common.enumeration.Status;
import online.longlian.common.enumeration.TaskTemplateScope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class WorkshopQueryBuilder {

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
