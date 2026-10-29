package online.longlian.app.service.orgadmin;

import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetFieldCreateParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetFieldDeleteParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetFieldListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetFieldListResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetFieldOptionBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetFieldUpdateParamsBO;

import java.util.List;

/**
 * 组织预置表单字段。管理端维护字段名；用户端按创建时间选择，值由填写企划的人补充。
 */
public interface OrganizationPresetFieldService {

    PageResultBO<OrganizationPresetFieldListResultBO> listPresetFields(OrganizationPresetFieldListParamsBO params);

    void createPresetField(OrganizationPresetFieldCreateParamsBO params);

    void updatePresetField(OrganizationPresetFieldUpdateParamsBO params);

    void deletePresetField(OrganizationPresetFieldDeleteParamsBO params);

    /**
     * 当前组织全部未删除预置字段，按创建时间倒序、ID 升序。
     */
    List<OrganizationPresetFieldOptionBO> listPresetFieldOptions(Long orgId);

    /**
     * 只返回仍属于该组织的预置字段。缺失的 ID 不会出现在结果里。
     */
    List<OrganizationPresetFieldOptionBO> listExisting(Long orgId, List<Long> fieldIds);
}
