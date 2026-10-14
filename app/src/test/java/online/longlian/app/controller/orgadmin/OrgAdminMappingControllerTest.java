package online.longlian.app.controller.orgadmin;

import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminApplicationInfoResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberBaseTaskSubmitCountItemBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberBaseTaskSubmitCountResultBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateListNodeResultBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateListResultBO;
import online.longlian.app.pojo.dto.orgadmin.ApplicationListDTO;
import online.longlian.app.pojo.dto.orgadmin.TaskTemplateListDTO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.pojo.vo.orgadmin.ApplicationInfoVO;
import online.longlian.app.pojo.vo.orgadmin.OrgMemberBaseTaskSubmitCountVO;
import online.longlian.app.pojo.vo.orgadmin.TaskTemplateListVO;
import online.longlian.app.service.orgadmin.OrganizationMemberService;
import online.longlian.app.service.orgadmin.TaskTemplateService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrgAdminMappingControllerTest {

    @Test
    void shouldMapApplicationAndSubmitCountRows() {
        OrganizationMemberService organizationMemberService = mock(OrganizationMemberService.class);
        when(organizationMemberService.listApplications(any())).thenReturn(new PageResultBO<>(
                List.of(OrgAdminApplicationInfoResultBO.builder().id(4L).nickname("申请人").build()), 1L));
        when(organizationMemberService.getMemberBaseTaskSubmitCounts(any())).thenReturn(
                OrgMemberBaseTaskSubmitCountResultBO.builder()
                        .memberId(2L)
                        .userId(20L)
                        .totalSubmitCount(3)
                        .items(List.of(OrgMemberBaseTaskSubmitCountItemBO.builder()
                                .baseTaskId(9L)
                                .baseTaskName("原子任务")
                                .submitCount(3)
                                .build()))
                        .build());
        OrganizationMemberController controller = new OrganizationMemberController(organizationMemberService);
        SessionContext session = new SessionContext(1L, 1L);

        PageResultVO<ApplicationInfoVO> applications = controller.listApplications(session, new ApplicationListDTO());
        OrgMemberBaseTaskSubmitCountVO counts = controller.getMemberBaseTaskSubmitCounts(session, 2L);

        assertThat(applications.getList()).extracting(ApplicationInfoVO::getNickname).containsExactly("申请人");
        assertThat(counts.getList()).singleElement().satisfies(item -> {
            assertThat(item.getBaseTaskId()).isEqualTo(9L);
            assertThat(item.getBaseTaskName()).isEqualTo("原子任务");
            assertThat(item.getSubmitCount()).isEqualTo(3);
        });
    }

    @Test
    void shouldMapTaskTemplateRows() {
        TaskTemplateService taskTemplateService = mock(TaskTemplateService.class);
        when(taskTemplateService.listTaskTemplates(any())).thenReturn(new PageResultBO<>(
                List.of(
                        TaskTemplateListResultBO.builder()
                                .id(6L)
                                .name("模板")
                                .taskCount(1)
                                .nodes(List.of(TaskTemplateListNodeResultBO.builder()
                                        .baseTaskId(9L)
                                        .baseTaskName("翻译")
                                        .baseTaskIcon("Languages")
                                        .sort(1)
                                        .parallelSort(2)
                                        .build()))
                                .build(),
                        TaskTemplateListResultBO.builder().id(7L).name("空").build()),
                2L));
        TaskTemplateController controller = new TaskTemplateController(taskTemplateService);

        PageResultVO<TaskTemplateListVO> page = controller.listTaskTemplates(
                new SessionContext(1L, 1L), new TaskTemplateListDTO());

        assertThat(page.getList()).satisfiesExactly(
                row -> {
                    assertThat(row.getName()).isEqualTo("模板");
                    assertThat(row.getTaskCount()).isEqualTo(1);
                    assertThat(row.getNodes()).singleElement().satisfies(node -> {
                        assertThat(node.getBaseTaskId()).isEqualTo(9L);
                        assertThat(node.getBaseTaskName()).isEqualTo("翻译");
                        assertThat(node.getBaseTaskIcon()).isEqualTo("Languages");
                        assertThat(node.getSort()).isEqualTo(1);
                        assertThat(node.getParallelSort()).isEqualTo(2);
                    });
                },
                row -> {
                    assertThat(row.getName()).isEqualTo("空");
                    assertThat(row.getTaskCount()).isNull();
                    assertThat(row.getNodes()).isEmpty();
                });
    }
}
