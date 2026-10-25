package online.longlian.app.service.orgadmin.impl.presettag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetTagListParamsBO;
import online.longlian.app.pojo.entity.OrganizationPresetTag;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OrganizationPresetTagQueryBuilder {

    public LambdaQueryWrapper<OrganizationPresetTag> buildListQuery(OrganizationPresetTagListParamsBO params) {
        LambdaQueryWrapper<OrganizationPresetTag> queryWrapper = new LambdaQueryWrapper<OrganizationPresetTag>()
                .eq(OrganizationPresetTag::getOrgId, params.getOrgId());

        if (StringUtils.hasText(params.getKeyword())) {
            queryWrapper.like(OrganizationPresetTag::getName, params.getKeyword().trim());
        }

        if (params.getOrderDir() == SortDirection.ASC) {
            queryWrapper.orderByAsc(OrganizationPresetTag::getCreatedAt);
        } else {
            queryWrapper.orderByDesc(OrganizationPresetTag::getCreatedAt);
        }

        return queryWrapper;
    }
}
