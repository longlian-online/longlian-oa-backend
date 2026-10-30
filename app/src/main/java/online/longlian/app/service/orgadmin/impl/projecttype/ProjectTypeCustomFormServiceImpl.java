package online.longlian.app.service.orgadmin.impl.projecttype;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ProjectTypeCustomFieldMapper;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetFieldOptionBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeCustomFieldBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeCustomFieldResultBO;
import online.longlian.app.pojo.entity.ProjectTypeCustomField;
import online.longlian.app.service.orgadmin.OrganizationPresetFieldService;
import online.longlian.app.service.orgadmin.ProjectTypeCustomFormService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectTypeCustomFormServiceImpl implements ProjectTypeCustomFormService {

    private final ProjectTypeCustomFieldMapper projectTypeCustomFieldMapper;
    private final OrganizationPresetFieldService organizationPresetFieldService;
    private final Clock clock;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replace(Long orgId, Long typeId, List<ProjectTypeCustomFieldBO> fields) {
        if (fields == null) {
            return;
        }
        List<Long> presetFieldIds = fields.stream()
                .map(ProjectTypeCustomFieldBO::getPresetFieldId)
                .filter(Objects::nonNull)
                .toList();
        if (presetFieldIds.stream().distinct().count() != presetFieldIds.size()) {
            throw new AppException(ResultCode.PARAM_ERROR, "默认自定义表单不能重复选择同一字段");
        }
        Map<Long, OrganizationPresetFieldOptionBO> presets = presetFieldIds.isEmpty()
                ? Map.of()
                : organizationPresetFieldService.listExisting(orgId, presetFieldIds).stream()
                        .collect(Collectors.toMap(OrganizationPresetFieldOptionBO::getId, Function.identity()));
        if (presets.size() != presetFieldIds.size()) {
            throw new AppException(ResultCode.PARAM_ERROR, "预置表单字段不存在");
        }

        List<String> customNames = new ArrayList<>(fields.size());
        for (ProjectTypeCustomFieldBO field : fields) {
            if (field.getPresetFieldId() != null) {
                customNames.add(null);
                continue;
            }
            String customName = field.getName() == null ? "" : field.getName().trim();
            if (customName.isEmpty()) {
                throw new AppException(ResultCode.PARAM_ERROR, "字段名不能为空");
            }
            customNames.add(customName);
        }

        projectTypeCustomFieldMapper.delete(new LambdaQueryWrapper<ProjectTypeCustomField>()
                .eq(ProjectTypeCustomField::getOrgId, orgId)
                .eq(ProjectTypeCustomField::getTypeId, typeId));
        LocalDateTime now = LocalDateTime.now(clock);
        for (int index = 0; index < fields.size(); index++) {
            ProjectTypeCustomFieldBO field = fields.get(index);
            projectTypeCustomFieldMapper.insert(ProjectTypeCustomField.builder()
                    .orgId(orgId)
                    .typeId(typeId)
                    .presetFieldId(field.getPresetFieldId())
                    .name(customNames.get(index))
                    .required(Boolean.TRUE.equals(field.getRequired()))
                    .sort(index)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
        }
    }

    @Override
    public Map<Long, List<ProjectTypeCustomFieldResultBO>> listByTypeIds(Long orgId, List<Long> typeIds) {
        if (typeIds.isEmpty()) {
            return Map.of();
        }
        List<ProjectTypeCustomField> rows = projectTypeCustomFieldMapper.selectList(
                new LambdaQueryWrapper<ProjectTypeCustomField>()
                        .eq(ProjectTypeCustomField::getOrgId, orgId)
                        .in(ProjectTypeCustomField::getTypeId, typeIds)
                        .orderByAsc(ProjectTypeCustomField::getSort)
                        .orderByAsc(ProjectTypeCustomField::getId));
        List<Long> presetFieldIds = rows.stream()
                .map(ProjectTypeCustomField::getPresetFieldId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, OrganizationPresetFieldOptionBO> presets = presetFieldIds.isEmpty()
                ? Map.of()
                : organizationPresetFieldService.listExisting(orgId, presetFieldIds).stream()
                        .collect(Collectors.toMap(OrganizationPresetFieldOptionBO::getId, Function.identity()));
        Map<Long, List<ProjectTypeCustomFieldResultBO>> result = new LinkedHashMap<>();
        for (ProjectTypeCustomField row : rows) {
            String name;
            if (row.getPresetFieldId() != null) {
                OrganizationPresetFieldOptionBO preset = presets.get(row.getPresetFieldId());
                name = preset != null ? preset.getName() : row.getName();
            } else {
                name = row.getName();
            }
            if (name == null || name.isBlank()) {
                continue;
            }
            result.computeIfAbsent(row.getTypeId(), ignored -> new ArrayList<>())
                    .add(ProjectTypeCustomFieldResultBO.builder()
                            .presetFieldId(row.getPresetFieldId())
                            .name(name)
                            .required(Boolean.TRUE.equals(row.getRequired()))
                            .build());
        }
        return result;
    }

    @Override
    public List<String> requiredFieldNames(Long orgId, Long typeId) {
        return listByTypeIds(orgId, List.of(typeId)).getOrDefault(typeId, List.of()).stream()
                .filter(field -> Boolean.TRUE.equals(field.getRequired()))
                .map(ProjectTypeCustomFieldResultBO::getName)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByType(Long orgId, Long typeId) {
        projectTypeCustomFieldMapper.delete(new LambdaQueryWrapper<ProjectTypeCustomField>()
                .eq(ProjectTypeCustomField::getOrgId, orgId)
                .eq(ProjectTypeCustomField::getTypeId, typeId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void detachPresetField(Long orgId, Long presetFieldId) {
        projectTypeCustomFieldMapper.delete(new LambdaQueryWrapper<ProjectTypeCustomField>()
                .eq(ProjectTypeCustomField::getOrgId, orgId)
                .eq(ProjectTypeCustomField::getPresetFieldId, presetFieldId));
    }
}
