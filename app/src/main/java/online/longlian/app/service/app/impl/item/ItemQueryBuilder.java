package online.longlian.app.service.app.impl.item;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.pojo.bo.app.ItemListParamsBO;
import online.longlian.app.pojo.entity.Item;
import online.longlian.common.enumeration.ItemStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class ItemQueryBuilder {

    public LambdaQueryWrapper<Item> buildListQuery(ItemListParamsBO params) {
        LambdaQueryWrapper<Item> queryWrapper = new LambdaQueryWrapper<Item>()
                .eq(Item::getProjectId, params.getProjectId());
        if (params.getStatus() == ItemStatus.COMPLETED || params.getStatus() == ItemStatus.IN_PROGRESS) {
            queryWrapper.eq(Item::getStatus, params.getStatus());
        }

        if (StringUtils.hasText(params.getKeyword())) {
            queryWrapper.like(Item::getTitle, params.getKeyword().trim());
        }

        switch (params.getSortBy()) {
            case MANUAL -> queryWrapper.orderByAsc(Item::getSort, Item::getId);
            case CREATED_ASC -> queryWrapper.orderByAsc(Item::getCreatedAt, Item::getId);
            case CREATED_DESC -> queryWrapper.orderByDesc(Item::getCreatedAt, Item::getId);
            case UPDATED_ASC -> queryWrapper.orderByAsc(Item::getUpdatedAt, Item::getId);
            case UPDATED_DESC -> queryWrapper.orderByDesc(Item::getUpdatedAt, Item::getId);
        }

        return queryWrapper;
    }
}
