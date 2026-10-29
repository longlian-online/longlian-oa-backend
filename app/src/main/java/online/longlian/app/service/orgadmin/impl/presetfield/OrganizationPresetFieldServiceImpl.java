package online.longlian.app.service.orgadmin.impl.presetfield;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.OrganizationPresetFieldMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetFieldCreateParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetFieldDeleteParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetFieldListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetFieldListResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetFieldOptionBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetFieldUpdateParamsBO;
import online.longlian.app.pojo.entity.OrganizationPresetField;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.orgadmin.OrganizationPresetFieldService;
import online.longlian.app.service.orgadmin.ProjectTypeCustomFormService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationPresetFieldServiceImpl implements OrganizationPresetFieldService {

    private final OrganizationPresetFieldMapper organizationPresetFieldMapper;
    private final UserMapper userMapper;
    private final Clock clock;
    private final OrganizationPresetFieldQueryBuilder organizationPresetFieldQueryBuilder;
    private final ObjectProvider<ProjectTypeCustomFormService> projectTypeCustomFormService;

    @Override
    public PageResultBO<OrganizationPresetFieldListResultBO> listPresetFields(OrganizationPresetFieldListParamsBO params) {
        Page<OrganizationPresetField> page = new Page<>(params.getPage().getPageNum(), params.getPage().getPageSize());
        Page<OrganizationPresetField> fieldPage = organizationPresetFieldMapper.selectPage(
                page, organizationPresetFieldQueryBuilder.buildListQuery(params));
        Map<Long, User> creators = loadCreators(fieldPage.getRecords());
        List<OrganizationPresetFieldListResultBO> list = fieldPage.getRecords().stream()
                .map(field -> OrganizationPresetFieldListResultBO.builder()
                        .id(field.getId())
                        .name(field.getName())
                        .createdAt(field.getCreatedAt())
                        .creatorNickname(nicknameOf(creators.get(field.getCreatorId())))
                        .build())
                .toList();
        return new PageResultBO<>(list, fieldPage.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPresetField(OrganizationPresetFieldCreateParamsBO params) {
        LocalDateTime now = LocalDateTime.now(clock);
        OrganizationPresetField field = OrganizationPresetField.builder()
                .orgId(params.getOrgId())
                .name(params.getName().trim())
                .creatorId(params.getCreatorId())
                .createdAt(now)
                .updatedAt(now)
                .build();
        organizationPresetFieldMapper.insert(field);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePresetField(OrganizationPresetFieldUpdateParamsBO params) {
        OrganizationPresetField field = requireOwnedField(params.getFieldId(), params.getOrgId());
        organizationPresetFieldMapper.update(null, new LambdaUpdateWrapper<OrganizationPresetField>()
                .eq(OrganizationPresetField::getId, field.getId())
                .set(OrganizationPresetField::getName, params.getName().trim())
                .set(OrganizationPresetField::getUpdatedAt, LocalDateTime.now(clock)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePresetField(OrganizationPresetFieldDeleteParamsBO params) {
        OrganizationPresetField field = requireOwnedField(params.getFieldId(), params.getOrgId());
        organizationPresetFieldMapper.deleteById(field.getId());
        projectTypeCustomFormService.getObject().detachPresetField(params.getOrgId(), field.getId());
    }

    @Override
    public List<OrganizationPresetFieldOptionBO> listPresetFieldOptions(Long orgId) {
        return organizationPresetFieldMapper.selectList(new LambdaQueryWrapper<OrganizationPresetField>()
                        .eq(OrganizationPresetField::getOrgId, orgId)
                        .orderByDesc(OrganizationPresetField::getCreatedAt)
                        .orderByAsc(OrganizationPresetField::getId))
                .stream()
                .map(this::toOption)
                .toList();
    }

    @Override
    public List<OrganizationPresetFieldOptionBO> listExisting(Long orgId, List<Long> fieldIds) {
        if (fieldIds.isEmpty()) {
            return List.of();
        }
        return organizationPresetFieldMapper.selectList(new LambdaQueryWrapper<OrganizationPresetField>()
                        .eq(OrganizationPresetField::getOrgId, orgId)
                        .in(OrganizationPresetField::getId, fieldIds))
                .stream()
                .map(this::toOption)
                .toList();
    }

    private Map<Long, User> loadCreators(List<OrganizationPresetField> fields) {
        List<Long> creatorIds = fields.stream().map(OrganizationPresetField::getCreatorId).distinct().toList();
        if (creatorIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(creatorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private String nicknameOf(User user) {
        if (user == null || !StringUtils.hasText(user.getNickname())) {
            return null;
        }
        return user.getNickname();
    }

    private OrganizationPresetField requireOwnedField(Long fieldId, Long orgId) {
        OrganizationPresetField field = organizationPresetFieldMapper.selectById(fieldId);
        if (field == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "预置表单字段不存在");
        }
        if (!field.getOrgId().equals(orgId)) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "无权操作该预置表单字段");
        }
        return field;
    }

    private OrganizationPresetFieldOptionBO toOption(OrganizationPresetField field) {
        return OrganizationPresetFieldOptionBO.builder()
                .id(field.getId())
                .name(field.getName())
                .build();
    }
}
