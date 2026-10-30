package online.longlian.app.service.orgadmin;

import online.longlian.app.pojo.bo.orgadmin.ProjectTypeCustomFieldBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeCustomFieldResultBO;

import java.util.List;
import java.util.Map;

/**
 * 企划类型的默认自定义表单。字段引用本组织的预置表单字段，并记录是否必填。
 */
public interface ProjectTypeCustomFormService {

    /**
     * {@code fields} 为 null 时保持原表单；空列表清空。
     */
    void replace(Long orgId, Long typeId, List<ProjectTypeCustomFieldBO> fields);

    Map<Long, List<ProjectTypeCustomFieldResultBO>> listByTypeIds(Long orgId, List<Long> typeIds);

    List<String> requiredFieldNames(Long orgId, Long typeId);

    void deleteByType(Long orgId, Long typeId);

    void detachPresetField(Long orgId, Long presetFieldId);
}
