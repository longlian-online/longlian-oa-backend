package online.longlian.app.service.orgadmin.impl.presettag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.OrganizationPresetTagMapper;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetTagCreateParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetTagDeleteParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetTagListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetTagListResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetTagOptionBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetTagUpdateParamsBO;
import online.longlian.app.pojo.entity.OrganizationPresetTag;
import online.longlian.app.service.orgadmin.OrganizationPresetTagService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrganizationPresetTagServiceImpl implements OrganizationPresetTagService {

    private static final Set<String> COLORS = Set.of("blue", "green", "amber", "rose", "violet");

    private final OrganizationPresetTagMapper organizationPresetTagMapper;
    private final Clock clock;
    private final OrganizationPresetTagQueryBuilder organizationPresetTagQueryBuilder;

    @Override
    public PageResultBO<OrganizationPresetTagListResultBO> listPresetTags(OrganizationPresetTagListParamsBO params) {
        Page<OrganizationPresetTag> page = new Page<>(params.getPage().getPageNum(), params.getPage().getPageSize());
        Page<OrganizationPresetTag> tagPage = organizationPresetTagMapper.selectPage(
                page, organizationPresetTagQueryBuilder.buildListQuery(params));
        List<OrganizationPresetTagListResultBO> list = tagPage.getRecords().stream()
                .map(tag -> OrganizationPresetTagListResultBO.builder()
                        .id(tag.getId())
                        .name(tag.getName())
                        .color(tag.getColor())
                        .priority(tag.getPriority())
                        .build())
                .toList();
        return new PageResultBO<>(list, tagPage.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPresetTag(OrganizationPresetTagCreateParamsBO params) {
        LocalDateTime now = LocalDateTime.now(clock);
        OrganizationPresetTag tag = OrganizationPresetTag.builder()
                .orgId(params.getOrgId())
                .name(params.getName().trim())
                .color(normalizeColor(params.getColor()))
                .priority(params.getPriority() == null ? 0 : params.getPriority())
                .creatorId(params.getCreatorId())
                .createdAt(now)
                .updatedAt(now)
                .build();
        organizationPresetTagMapper.insert(tag);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePresetTag(OrganizationPresetTagUpdateParamsBO params) {
        OrganizationPresetTag tag = requireOwnedTag(params.getTagId(), params.getOrgId());
        LambdaUpdateWrapper<OrganizationPresetTag> update = new LambdaUpdateWrapper<OrganizationPresetTag>()
                .eq(OrganizationPresetTag::getId, tag.getId())
                .set(OrganizationPresetTag::getName, params.getName().trim())
                .set(OrganizationPresetTag::getColor, normalizeColor(params.getColor()))
                .set(params.getPriority() != null, OrganizationPresetTag::getPriority, params.getPriority())
                .set(OrganizationPresetTag::getUpdatedAt, LocalDateTime.now(clock));
        organizationPresetTagMapper.update(null, update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePresetTag(OrganizationPresetTagDeleteParamsBO params) {
        OrganizationPresetTag tag = requireOwnedTag(params.getTagId(), params.getOrgId());
        organizationPresetTagMapper.deleteById(tag.getId());
    }

    @Override
    public List<OrganizationPresetTagOptionBO> listPresetTagOptions(Long orgId) {
        return organizationPresetTagMapper.selectList(new LambdaQueryWrapper<OrganizationPresetTag>()
                        .eq(OrganizationPresetTag::getOrgId, orgId)
                        .orderByDesc(OrganizationPresetTag::getPriority)
                        .orderByAsc(OrganizationPresetTag::getId))
                .stream()
                .map(tag -> OrganizationPresetTagOptionBO.builder()
                        .id(tag.getId())
                        .name(tag.getName())
                        .color(tag.getColor())
                        .build())
                .toList();
    }

    private OrganizationPresetTag requireOwnedTag(Long tagId, Long orgId) {
        OrganizationPresetTag tag = organizationPresetTagMapper.selectById(tagId);
        if (tag == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "预制标签不存在");
        }
        if (!tag.getOrgId().equals(orgId)) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "无权操作该预制标签");
        }
        return tag;
    }

    private String normalizeColor(String color) {
        if (color == null || color.isBlank()) {
            return null;
        }
        String normalized = color.trim();
        if (!COLORS.contains(normalized)) {
            throw new AppException(ResultCode.PARAM_ERROR, "颜色必须是 blue、green、amber、rose 或 violet");
        }
        return normalized;
    }
}
