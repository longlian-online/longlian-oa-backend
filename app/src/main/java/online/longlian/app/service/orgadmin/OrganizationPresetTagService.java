package online.longlian.app.service.orgadmin;

import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetTagCreateParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetTagDeleteParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetTagListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetTagListResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetTagOptionBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetTagUpdateParamsBO;

import java.util.List;

/**
 * 组织预制标签。管理端维护标签名、颜色和优先级；用户端按下拉顺序读取，值由填写企划的人补充。
 */
public interface OrganizationPresetTagService {

    PageResultBO<OrganizationPresetTagListResultBO> listPresetTags(OrganizationPresetTagListParamsBO params);

    void createPresetTag(OrganizationPresetTagCreateParamsBO params);

    void updatePresetTag(OrganizationPresetTagUpdateParamsBO params);

    void deletePresetTag(OrganizationPresetTagDeleteParamsBO params);

    /**
     * 当前组织全部未删除预制标签，按优先级降序、ID 升序。
     */
    List<OrganizationPresetTagOptionBO> listPresetTagOptions(Long orgId);
}
