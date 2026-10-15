package online.longlian.app.service.app.impl.item;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.enumeration.SortByTime;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.pojo.bo.app.ItemListParamsBO;
import online.longlian.app.pojo.entity.Item;
import online.longlian.common.enumeration.ItemStatus;
import online.longlian.common.enumeration.TaskInstanceStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class ItemQueryBuilder {

    /**
     * 至少有一个未删除任务，且不存在未完成任务。关联外层 item 表。
     */
    private static final String ALL_TASKS_COMPLETED = """
            EXISTS (SELECT 1 FROM task_instance ti WHERE ti.item_id = item.id AND ti.deleted_at IS NULL)
            AND NOT EXISTS (
                SELECT 1 FROM task_instance ti
                WHERE ti.item_id = item.id AND ti.deleted_at IS NULL AND ti.status <> {0}
            )
            """;

    public LambdaQueryWrapper<Item> buildListQuery(ItemListParamsBO params) {
        LambdaQueryWrapper<Item> queryWrapper = new LambdaQueryWrapper<Item>()
                .eq(Item::getProjectId, params.getProjectId());
        if (params.getStatus() == ItemStatus.COMPLETED) {
            queryWrapper.apply(ALL_TASKS_COMPLETED, TaskInstanceStatus.COMPLETED.getCode());
        } else if (params.getStatus() == ItemStatus.IN_PROGRESS) {
            queryWrapper.apply("NOT (" + ALL_TASKS_COMPLETED + ")", TaskInstanceStatus.COMPLETED.getCode());
        }

        if (StringUtils.hasText(params.getKeyword())) {
            queryWrapper.like(Item::getTitle, params.getKeyword().trim());
        }

        if (params.getSortByTime() == SortByTime.UPDATE) {
            queryWrapper.orderBy(true, params.getOrderDir() == SortDirection.ASC, Item::getUpdatedAt);
        } else {
            queryWrapper.orderBy(true, params.getOrderDir() == SortDirection.ASC, Item::getCreatedAt);
        }

        return queryWrapper;
    }
}
