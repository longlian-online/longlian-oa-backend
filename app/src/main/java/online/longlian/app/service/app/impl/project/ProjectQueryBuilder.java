package online.longlian.app.service.app.impl.project;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.enumeration.SortByTime;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.pojo.bo.app.ProjectListParamsBO;
import online.longlian.app.pojo.entity.Project;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class ProjectQueryBuilder {

    public LambdaQueryWrapper<Project> buildListQuery(ProjectListParamsBO params, Long typeId) {
        LambdaQueryWrapper<Project> queryWrapper = new LambdaQueryWrapper<Project>()
                .eq(Project::getOrgId, params.getOrgId())
                .eq(typeId != null, Project::getTypeId, typeId);

        if (StringUtils.hasText(params.getKeyword())) {
            queryWrapper.like(Project::getTitle, params.getKeyword().trim());
        }

        if (params.getSortByTime() == SortByTime.UPDATE) {
            if (params.getOrderDir() == SortDirection.ASC) {
                queryWrapper.orderByAsc(Project::getUpdatedAt);
            } else {
                queryWrapper.orderByDesc(Project::getUpdatedAt);
            }
        } else {
            if (params.getOrderDir() == SortDirection.ASC) {
                queryWrapper.orderByAsc(Project::getCreatedAt);
            } else {
                queryWrapper.orderByDesc(Project::getCreatedAt);
            }
        }

        return queryWrapper;
    }
}
