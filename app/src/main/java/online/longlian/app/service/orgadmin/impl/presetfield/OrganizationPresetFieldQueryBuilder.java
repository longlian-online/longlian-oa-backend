package online.longlian.app.service.orgadmin.impl.presetfield;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetFieldListParamsBO;
import online.longlian.app.pojo.entity.OrganizationPresetField;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OrganizationPresetFieldQueryBuilder {

    public LambdaQueryWrapper<OrganizationPresetField> buildListQuery(OrganizationPresetFieldListParamsBO params) {
        LambdaQueryWrapper<OrganizationPresetField> queryWrapper = new LambdaQueryWrapper<OrganizationPresetField>()
                .eq(OrganizationPresetField::getOrgId, params.getOrgId());

        if (StringUtils.hasText(params.getKeyword())) {
            queryWrapper.like(OrganizationPresetField::getName, params.getKeyword().trim());
        }

        if (params.getOrderDir() == SortDirection.ASC) {
            queryWrapper.orderByAsc(OrganizationPresetField::getCreatedAt);
        } else {
            queryWrapper.orderByDesc(OrganizationPresetField::getCreatedAt);
        }

        return queryWrapper;
    }
}
