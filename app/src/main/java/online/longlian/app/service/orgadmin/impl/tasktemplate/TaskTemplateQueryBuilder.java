package online.longlian.app.service.orgadmin.impl.tasktemplate;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.common.enumeration.TaskTemplateSortBy;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateListParamsBO;
import online.longlian.app.pojo.entity.TaskTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class TaskTemplateQueryBuilder {

    public LambdaQueryWrapper<TaskTemplate> buildListQuery(TaskTemplateListParamsBO params) {
        LambdaQueryWrapper<TaskTemplate> queryWrapper = new LambdaQueryWrapper<TaskTemplate>()
                .eq(TaskTemplate::getOrgId, params.getOrgId())
                .eq(params.getStatus() != null, TaskTemplate::getStatus, params.getStatus())
                .ge(params.getStartCreatedTime() != null, TaskTemplate::getCreatedAt, params.getStartCreatedTime())
                .le(params.getEndCreatedTime() != null, TaskTemplate::getCreatedAt, params.getEndCreatedTime());

        if (StringUtils.hasText(params.getKeyword())) {
            queryWrapper.like(TaskTemplate::getName, params.getKeyword().trim());
        }

        if (params.getSortBy() == TaskTemplateSortBy.CREATED_AT) {
            queryWrapper.orderBy(true, params.getOrderDir() == SortDirection.ASC, TaskTemplate::getCreatedAt);
        } else {
            queryWrapper.orderBy(true, params.getOrderDir() == SortDirection.ASC, TaskTemplate::getRefCount);
        }

        return queryWrapper;
    }
}
