package online.longlian.app.service.admin.impl;

import lombok.RequiredArgsConstructor;
import online.longlian.app.pojo.bo.admin.AdminOrganizationIdentityBO;
import online.longlian.app.pojo.bo.admin.AdminOrganizationListParamsBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.common.ResourceUsageSummaryBO;
import online.longlian.app.pojo.vo.admin.StorageCategoryStatsVO;
import online.longlian.app.pojo.vo.admin.StorageStateStatsVO;
import online.longlian.app.pojo.vo.admin.StorageStatsVO;
import online.longlian.app.service.admin.OrganizationService;
import online.longlian.app.service.admin.StorageStatsService;
import online.longlian.app.service.resource.ResourceService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StorageStatsServiceImpl implements StorageStatsService {
    private final OrganizationService organizationService;
    private final ResourceService resourceService;

    @Override
    public PageResultBO<StorageStatsVO> list(AdminOrganizationListParamsBO params) {
        PageResultBO<AdminOrganizationIdentityBO> organizations = organizationService.pageIdentities(params);
        if (organizations.getList().isEmpty()) {
            return new PageResultBO<>(List.of(), organizations.getTotal());
        }
        Map<Long, Map<String, Map<String, ResourceUsageSummaryBO>>> usage = index(resourceService.summarizeUsage(
                organizations.getList().stream().map(AdminOrganizationIdentityBO::getId).toList()));
        List<StorageStatsVO> list = organizations.getList().stream()
                .map(organization -> toStats(organization, usage.getOrDefault(organization.getId(), Map.of())))
                .toList();
        return new PageResultBO<>(list, organizations.getTotal());
    }

    private Map<Long, Map<String, Map<String, ResourceUsageSummaryBO>>> index(List<ResourceUsageSummaryBO> rows) {
        Map<Long, Map<String, Map<String, ResourceUsageSummaryBO>>> usage = new HashMap<>();
        for (ResourceUsageSummaryBO row : rows) {
            usage.computeIfAbsent(row.getOrgId(), ignored -> new HashMap<>())
                    .computeIfAbsent(row.getBizType(), ignored -> new HashMap<>())
                    .put(row.getState(), row);
        }
        return usage;
    }

    private StorageStatsVO toStats(AdminOrganizationIdentityBO organization,
                                   Map<String, Map<String, ResourceUsageSummaryBO>> byType) {
        StorageCategoryStatsVO cover = category(byType.get("cover"));
        StorageCategoryStatsVO thumbnail = category(byType.get("thumbnail"));
        StorageCategoryStatsVO taskSubmit = category(byType.get("task_submit"));
        return new StorageStatsVO(
                organization.getId(),
                organization.getName(),
                organization.getStatus(),
                occupied(cover) + occupied(thumbnail) + occupied(taskSubmit),
                reclaimed(cover) + reclaimed(thumbnail) + reclaimed(taskSubmit),
                cover,
                thumbnail,
                taskSubmit);
    }

    private StorageCategoryStatsVO category(Map<String, ResourceUsageSummaryBO> byState) {
        return new StorageCategoryStatsVO(
                state(byState, "unbound"),
                state(byState, "active"),
                state(byState, "retention"),
                state(byState, "recycled"));
    }

    private StorageStateStatsVO state(Map<String, ResourceUsageSummaryBO> byState, String name) {
        if (byState == null) {
            return new StorageStateStatsVO(0, 0);
        }
        ResourceUsageSummaryBO row = byState.get(name);
        if (row == null) {
            return new StorageStateStatsVO(0, 0);
        }
        long count = row.getFileCount() == null ? 0 : row.getFileCount();
        long bytes = row.getBytes() == null ? 0 : row.getBytes();
        return new StorageStateStatsVO(count, bytes);
    }

    private long occupied(StorageCategoryStatsVO category) {
        return category.getUnbound().getBytes() + category.getActive().getBytes() + category.getRetention().getBytes();
    }

    private long reclaimed(StorageCategoryStatsVO category) {
        return category.getRecycled().getBytes();
    }
}
