package online.longlian.app.service.app.impl.taskinstance;

import lombok.RequiredArgsConstructor;
import online.longlian.app.common.enumeration.MyTaskBucket;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.pojo.bo.app.ItemStageNodeStatusBO;
import online.longlian.app.pojo.bo.app.MyTaskParamsBO;
import online.longlian.app.pojo.bo.app.MyTaskRowBO;
import online.longlian.app.pojo.vo.app.MyTaskVO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.service.app.MyTaskService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 我的待办：SQL 只负责范围过滤（当前用户、组织、CLAIMED、企划启用、项目进行中且未删除）与按接取时间排序；
 * 分组判定统一走 {@link StageUnlockPolicy}，不在 SQL 中另写前置逻辑。
 * 单个用户同时接取的任务量有限，分组后在内存中分页。
 */
@Service
@RequiredArgsConstructor
public class MyTaskServiceImpl implements MyTaskService {

    private final TaskInstanceMapper taskInstanceMapper;

    @Override
    public PageResultVO<MyTaskVO> listMyTasks(MyTaskParamsBO params) {
        boolean desc = params.getSortDirection() == SortDirection.DESC;
        List<MyTaskRowBO> rows = taskInstanceMapper.selectMyClaimedTasks(params.getUserId(), params.getOrgId(), desc);
        if (rows.isEmpty()) {
            return new PageResultVO<>(Collections.emptyList(), 0L);
        }

        List<Long> itemIds = rows.stream().map(MyTaskRowBO::getItemId).distinct().toList();
        Map<Long, List<ItemStageNodeStatusBO>> nodesByItem = taskInstanceMapper.selectItemStageNodeStatuses(itemIds)
                .stream().collect(Collectors.groupingBy(ItemStageNodeStatusBO::getItemId));

        boolean wantWaiting = params.getBucket() == MyTaskBucket.WAITING;
        List<MyTaskVO> bucket = rows.stream()
                .map(row -> toVO(row, nodesByItem.getOrDefault(row.getItemId(), Collections.emptyList())))
                .filter(vo -> (vo.getWaitingStageName() != null) == wantWaiting)
                .toList();

        int pageNum = params.getPage().getPageNum();
        int pageSize = params.getPage().getPageSize();
        int from = Math.min((pageNum - 1) * pageSize, bucket.size());
        int to = Math.min(from + pageSize, bucket.size());
        return new PageResultVO<>(bucket.subList(from, to), (long) bucket.size());
    }

    private MyTaskVO toVO(MyTaskRowBO row, List<ItemStageNodeStatusBO> itemNodes) {
        String waitingStageName = null;
        if (!StageUnlockPolicy.isUnlocked(row.getStageSort(), itemNodes)) {
            waitingStageName = StageUnlockPolicy.nearestIncompletePredecessor(row.getStageSort(), itemNodes)
                    .map(ItemStageNodeStatusBO::getNodeName)
                    .orElseThrow();
        }
        return MyTaskVO.builder()
                .instanceId(row.getInstanceId())
                .itemId(row.getItemId())
                .projectId(row.getProjectId())
                .itemTitle(row.getItemTitle())
                .nodeName(row.getNodeName())
                .stageSort(row.getStageSort())
                .projectName(row.getProjectName())
                .claimedAt(row.getClaimedAt())
                .waitingStageName(waitingStageName)
                .build();
    }
}
