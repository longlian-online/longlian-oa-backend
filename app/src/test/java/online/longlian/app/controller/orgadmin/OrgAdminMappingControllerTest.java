package online.longlian.app.controller.orgadmin;

import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminApplicationInfoResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberBaseTaskSubmitCountItemBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberBaseTaskSubmitCountResultBO;
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
                List.of(TaskTemplateListResultBO.builder().id(6L).name("模板").build()), 1L));
        TaskTemplateController controller = new TaskTemplateController(taskTemplateService);

        PageResultVO<TaskTemplateListVO> page = controller.listTaskTemplates(
                new SessionContext(1L, 1L), new TaskTemplateListDTO());

        assertThat(page.getList()).extracting(TaskTemplateListVO::getName).containsExactly("模板");
    }
}
