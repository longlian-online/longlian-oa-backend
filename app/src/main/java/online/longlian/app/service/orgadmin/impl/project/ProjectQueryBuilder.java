package online.longlian.app.service.orgadmin.impl.project;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.pojo.bo.orgadmin.ProjectAdminListParamsBO;
import online.longlian.app.pojo.entity.Project;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class ProjectQueryBuilder {

    public LambdaQueryWrapper<Project> buildAdminListQuery(ProjectAdminListParamsBO params) {
        LambdaQueryWrapper<Project> queryWrapper = new LambdaQueryWrapper<Project>()
                .eq(Project::getOrgId, params.getOrgId())
                .eq(params.getTypeId() != null, Project::getTypeId, params.getTypeId())
                .ge(params.getStartCreatedTime() != null, Project::getCreatedAt, params.getStartCreatedTime())
                .le(params.getEndCreatedTime() != null, Project::getCreatedAt, params.getEndCreatedTime());

        if (StringUtils.hasText(params.getKeyword())) {
            queryWrapper.like(Project::getTitle, params.getKeyword().trim());
        }

        if (params.getOrderDir() == SortDirection.ASC) {
            queryWrapper.orderByAsc(Project::getCreatedAt);
        } else {
            queryWrapper.orderByDesc(Project::getCreatedAt);
        }

        return queryWrapper;
    }
}
