package online.longlian.app.service.orgadmin.impl.projecttype;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeListParamsBO;
import online.longlian.app.pojo.entity.ProjectType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class ProjectTypeQueryBuilder {

    public LambdaQueryWrapper<ProjectType> buildListQuery(ProjectTypeListParamsBO params) {
        LambdaQueryWrapper<ProjectType> queryWrapper = new LambdaQueryWrapper<ProjectType>()
                .eq(ProjectType::getOrgId, params.getOrgId());

        if (StringUtils.hasText(params.getKeyword())) {
            queryWrapper.like(ProjectType::getName, params.getKeyword().trim());
        }

        if (params.getOrderDir() == SortDirection.ASC) {
            queryWrapper.orderByAsc(ProjectType::getCreatedAt);
        } else {
            queryWrapper.orderByDesc(ProjectType::getCreatedAt);
        }

        return queryWrapper;
    }
}
