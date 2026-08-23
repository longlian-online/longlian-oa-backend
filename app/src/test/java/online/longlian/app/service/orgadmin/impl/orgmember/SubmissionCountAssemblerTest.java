package online.longlian.app.service.orgadmin.impl.orgmember;

import online.longlian.app.pojo.bo.orgadmin.OrgMemberBaseTaskSubmitCountResultBO;
import online.longlian.app.pojo.entity.BaseTask;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.TaskSubmission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SubmissionCountAssemblerTest {

    private SubmissionCountAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new SubmissionCountAssembler();
    }

    private OrganizationMember member(Long id, Long userId, int submitCount) {
        return OrganizationMember.builder().id(id).userId(userId).submitCount(submitCount).build();
    }

    @Test
    void assembleResult_withSubmissions_countsCorrectly() {
        OrganizationMember member = member(1L, 10L, 3);
        List<BaseTask> baseTasks = List.of(
                BaseTask.builder().id(100L).name("Task B").build(),
                BaseTask.builder().id(200L).name("Task A").build()
        );
        // node 1 -> baseTask 100, node 2 -> baseTask 200
        Map<Long, Long> nodeIdToBaseTaskId = Map.of(1L, 100L, 2L, 200L);
        List<TaskSubmission> submissions = List.of(
                TaskSubmission.builder().itemTaskNodeId(1L).build(),
                TaskSubmission.builder().itemTaskNodeId(1L).build(),
                TaskSubmission.builder().itemTaskNodeId(2L).build()
        );

        OrgMemberBaseTaskSubmitCountResultBO result = assembler.assembleResult(member, baseTasks, nodeIdToBaseTaskId, submissions);

        assertThat(result.getMemberId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(10L);
        assertThat(result.getTotalSubmitCount()).isEqualTo(3);
        assertThat(result.getItems()).hasSize(2);
        // sorted by name: Task A first
        assertThat(result.getItems().get(0).getBaseTaskName()).isEqualTo("Task A");
        assertThat(result.getItems().get(0).getSubmitCount()).isEqualTo(1);
        assertThat(result.getItems().get(1).getBaseTaskName()).isEqualTo("Task B");
        assertThat(result.getItems().get(1).getSubmitCount()).isEqualTo(2);
    }

    @Test
    void assembleResult_noSubmissions_allZero() {
        OrganizationMember member = member(1L, 10L, 0);
        List<BaseTask> baseTasks = List.of(BaseTask.builder().id(100L).name("Task").build());
        Map<Long, Long> nodeIdToBaseTaskId = Map.of(1L, 100L);

        OrgMemberBaseTaskSubmitCountResultBO result = assembler.assembleResult(member, baseTasks, nodeIdToBaseTaskId, Collections.emptyList());

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getSubmitCount()).isZero();
    }

    @Test
    void buildEmptyResult_withBaseTasks_returnsZeroCounts() {
        OrganizationMember member = member(1L, 10L, 0);
        List<BaseTask> baseTasks = List.of(
                BaseTask.builder().id(100L).name("Z Task").build(),
                BaseTask.builder().id(200L).name("A Task").build()
        );

        OrgMemberBaseTaskSubmitCountResultBO result = assembler.buildEmptyResult(member, baseTasks);

        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getItems().get(0).getBaseTaskName()).isEqualTo("A Task");
        assertThat(result.getItems().get(1).getBaseTaskName()).isEqualTo("Z Task");
        assertThat(result.getItems()).allSatisfy(item -> assertThat(item.getSubmitCount()).isZero());
    }

    @Test
    void buildEmptyResult_emptyBaseTasks_returnsEmptyItems() {
        OrganizationMember member = member(1L, 10L, 0);

        OrgMemberBaseTaskSubmitCountResultBO result = assembler.buildEmptyResult(member, Collections.emptyList());

        assertThat(result.getItems()).isEmpty();
    }
}
