package online.longlian.app.service.app.impl.item;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.enumeration.SortByTime;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.pojo.bo.app.ItemListParamsBO;
import online.longlian.app.pojo.entity.Item;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class ItemQueryBuilder {

    public LambdaQueryWrapper<Item> buildListQuery(ItemListParamsBO params) {
        LambdaQueryWrapper<Item> queryWrapper = new LambdaQueryWrapper<Item>()
                .eq(Item::getProjectId, params.getProjectId())
                .isNull(Item::getDeletedAt)
                .eq(params.getStatus() != null, Item::getStatus, params.getStatus());

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
