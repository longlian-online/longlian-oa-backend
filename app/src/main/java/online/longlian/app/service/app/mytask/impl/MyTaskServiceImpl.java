package online.longlian.app.service.app.mytask.impl;
import online.longlian.app.service.app.taskstage.SlotLabels;
import online.longlian.app.service.app.taskstage.StageUnlockPolicy;


import lombok.RequiredArgsConstructor;
import online.longlian.app.common.enumeration.MyTaskBucket;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.pojo.bo.app.taskinstance.ItemStageNodeStatusBO;
import online.longlian.app.pojo.bo.app.mytask.MyTaskKeysetItemBO;
import online.longlian.app.pojo.bo.app.mytask.MyTaskKeysetParamsBO;
import online.longlian.app.pojo.bo.app.mytask.MyTaskKeysetResultBO;
import online.longlian.app.pojo.bo.app.mytask.MyTaskParamsBO;
import online.longlian.app.pojo.bo.app.mytask.MyTaskResultBO;
import online.longlian.app.pojo.bo.app.mytask.MyTaskRowBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.service.app.mytask.MyTaskService;
import online.longlian.common.enumeration.TaskInstanceStatus;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Collections;
import java.util.stream.Stream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    public PageResultBO<MyTaskResultBO> listMyTasks(MyTaskParamsBO params) {
        boolean desc = params.getSortDirection() == SortDirection.DESC;
        List<MyTaskRowBO> rows = taskInstanceMapper.selectMyClaimedTasks(params.getUserId(), params.getOrgId(), desc);
        if (rows.isEmpty()) {
            return new PageResultBO<>(Collections.emptyList(), 0L);
        }

        List<Long> itemIds = rows.stream().map(MyTaskRowBO::getItemId).distinct().toList();
        Map<Long, List<ItemStageNodeStatusBO>> nodesByItem = taskInstanceMapper.selectItemStageNodeStatuses(itemIds)
                .stream().collect(Collectors.groupingBy(ItemStageNodeStatusBO::getItemId));

        boolean wantWaiting = params.getBucket() == MyTaskBucket.WAITING;
        List<MyTaskResultBO> bucket = rows.stream()
                .map(row -> toResultBO(row, nodesByItem.getOrDefault(row.getItemId(), Collections.emptyList())))
                .filter(resultBO -> (resultBO.getWaitingStageName() != null) == wantWaiting)
                .toList();

        int pageNum = params.getPage().getPageNum();
        int pageSize = params.getPage().getPageSize();
        int from = Math.min((pageNum - 1) * pageSize, bucket.size());
        int to = Math.min(from + pageSize, bucket.size());
        return new PageResultBO<>(bucket.subList(from, to), (long) bucket.size());
    }

    @Override
    public MyTaskKeysetResultBO listMyTasksKeyset(MyTaskKeysetParamsBO params) {
        // 与站内一致全量载入后分组（单用户同时接取的任务量有限），键集切片在内存完成
        List<MyTaskRowBO> rows = taskInstanceMapper.selectMyClaimedTasks(params.userId(), params.orgId(), false);
        if (rows.isEmpty()) {
            return new MyTaskKeysetResultBO(List.of(), null, null, false);
        }
        List<Long> itemIds = rows.stream().map(MyTaskRowBO::getItemId).distinct().toList();
        Map<Long, List<ItemStageNodeStatusBO>> nodesByItem = taskInstanceMapper.selectItemStageNodeStatuses(itemIds)
                .stream().collect(Collectors.groupingBy(ItemStageNodeStatusBO::getItemId));

        // SQL 已按 (claimed_at, id) 排序；nullsLast 与站内 ORDER BY 语义一致
        Comparator<MyTaskRowBO> order = Comparator
                .comparing(MyTaskRowBO::getClaimedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(MyTaskRowBO::getInstanceId);
        record Prepared(MyTaskRowBO row, MyTaskResultBO result) {
        }
        Stream<Prepared> prepared = rows.stream()
                .map(row -> new Prepared(row, toResultBO(row,
                        nodesByItem.getOrDefault(row.getItemId(), Collections.emptyList()))));
        if (params.bucket() != null) {
            boolean wantWaiting = params.bucket() == MyTaskBucket.WAITING;
            prepared = prepared.filter(entry -> (entry.result().getWaitingStageName() != null) == wantWaiting);
        }
        if (params.afterClaimedAt() != null) {
            MyTaskRowBO anchor = new MyTaskRowBO();
            anchor.setClaimedAt(params.afterClaimedAt());
            anchor.setInstanceId(params.afterInstanceId());
            prepared = prepared.filter(entry -> order.compare(entry.row(), anchor) > 0);
        }
        List<Prepared> window = prepared.limit((long) params.limit() + 1).toList();
        boolean hasMore = window.size() > params.limit();
        List<Prepared> page = hasMore ? window.subList(0, params.limit()) : window;
        if (page.isEmpty()) {
            return new MyTaskKeysetResultBO(List.of(), null, null, false);
        }
        List<MyTaskKeysetItemBO> items = page.stream()
                .map(entry -> new MyTaskKeysetItemBO(
                        entry.row().getInstanceId(),
                        entry.result().getNodeName(),
                        entry.row().getItemId(),
                        entry.result().getItemTitle(),
                        entry.row().getProjectId(),
                        entry.result().getProjectName(),
                        // SQL 固定筛选 status=2，开放面待办全部为 CLAIMED
                        TaskInstanceStatus.CLAIMED,
                        entry.row().getRevision(),
                        entry.row().getClaimedAt(),
                        entry.result().getWaitingStageName() != null ? MyTaskBucket.WAITING : MyTaskBucket.SUBMITTABLE))
                .toList();
        MyTaskRowBO last = page.get(page.size() - 1).row();
        return new MyTaskKeysetResultBO(items, last.getClaimedAt(), last.getInstanceId(), hasMore);
    }

    private MyTaskResultBO toResultBO(MyTaskRowBO row, List<ItemStageNodeStatusBO> itemNodes) {
        Optional<ItemStageNodeStatusBO> waiting = StageUnlockPolicy.nearestIncompletePredecessor(
                row.getStageSort(), itemNodes);
        SlotLabels.Position self = SlotLabels.of(row.getNodeId(), itemNodes);
        SlotLabels.Position blocked = waiting
                .map(node -> SlotLabels.of(node.getNodeId(), itemNodes))
                .orElse(null);
        return MyTaskResultBO.builder()
                .instanceId(row.getInstanceId())
                .itemId(row.getItemId())
                .projectId(row.getProjectId())
                .itemTitle(row.getItemTitle())
                .nodeName(row.getNodeName())
                .stageSort(row.getStageSort())
                .slotIndex(self.slotIndex())
                .slotTotal(self.slotTotal())
                .serialStep(self.serialStep())
                .projectName(row.getProjectName())
                .claimedAt(row.getClaimedAt())
                .waitingStageName(waiting.map(ItemStageNodeStatusBO::getNodeName).orElse(null))
                .waitingSlotIndex(blocked == null ? null : blocked.slotIndex())
                .waitingSlotTotal(blocked == null ? null : blocked.slotTotal())
                .waitingSerialStep(blocked == null ? null : blocked.serialStep())
                .build();
    }
}
