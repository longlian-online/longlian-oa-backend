package online.longlian.app.service.orgadmin.impl.basetask;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.enumeration.BaseTaskSortBy;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.pojo.bo.orgadmin.BaseTaskListParamsBO;
import online.longlian.app.pojo.entity.BaseTask;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class BaseTaskQueryBuilder {

    public LambdaQueryWrapper<BaseTask> buildListQuery(BaseTaskListParamsBO params) {
        LambdaQueryWrapper<BaseTask> queryWrapper = new LambdaQueryWrapper<BaseTask>()
                .eq(BaseTask::getOrgId, params.getOrgId())
                .eq(params.getStatus() != null, BaseTask::getStatus, params.getStatus())
                .ge(params.getStartCreatedTime() != null, BaseTask::getCreatedAt, params.getStartCreatedTime())
                .le(params.getEndCreatedTime() != null, BaseTask::getCreatedAt, params.getEndCreatedTime());

        if (StringUtils.hasText(params.getKeyword())) {
            queryWrapper.like(BaseTask::getName, params.getKeyword().trim());
        }

        if (params.getSortBy() == BaseTaskSortBy.CREATED_AT) {
            queryWrapper.orderBy(true, params.getOrderDir() == SortDirection.ASC, BaseTask::getCreatedAt);
        } else {
            queryWrapper.orderByDesc(BaseTask::getCreatedAt);
        }

        return queryWrapper;
    }
}
