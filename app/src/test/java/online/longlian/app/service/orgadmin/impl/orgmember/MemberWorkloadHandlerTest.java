package online.longlian.app.service.orgadmin.impl.orgmember;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.pojo.bo.app.ClaimedTaskLoadResultBO;
import online.longlian.app.pojo.bo.common.PageParamsBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberInfoResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberNoteParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberWorkloadListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberWorkloadResultBO;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.service.app.ClaimedTaskLoadService;
import online.longlian.common.enumeration.Status;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberWorkloadHandlerTest {

    @Mock
    private OrganizationMemberMapper organizationMemberMapper;
    @Mock
    private MemberQueryBuilder memberQueryBuilder;
    @Mock
    private MemberAssembler memberAssembler;
    @Mock
    private ClaimedTaskLoadService claimedTaskLoadService;

    private MemberWorkloadHandler handler;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), OrganizationMember.class);
        handler = new MemberWorkloadHandler(organizationMemberMapper, memberQueryBuilder, memberAssembler,
                claimedTaskLoadService);
    }

    @Test
    void listsBusiestMembersFirstAndCountsWaiting() {
        when(memberQueryBuilder.buildWorkloadMemberQuery(any())).thenReturn(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>());
        when(organizationMemberMapper.selectList(any())).thenReturn(List.of(member(1L, 11L), member(2L, 22L), member(3L, 33L)));
        when(claimedTaskLoadService.listClaimedTasks(any())).thenReturn(List.of(
                task(22L, 201L, null),
                task(22L, 202L, "翻译"),
                task(33L, 301L, null)));
        when(memberAssembler.assembleMembers(any())).thenAnswer(invocation -> {
            List<OrganizationMember> page = invocation.getArgument(0);
            return page.stream().map(item -> OrgMemberInfoResultBO.builder()
                    .id(item.getId()).userId(item.getUserId()).nickname("成员" + item.getId()).build()).toList();
        });

        PageResultBO<OrgMemberWorkloadResultBO> result = handler.list(params(SortDirection.DESC, 1, 2));

        assertThat(result.getTotal()).isEqualTo(3L);
        assertThat(result.getList()).extracting(OrgMemberWorkloadResultBO::getId).containsExactly(2L, 3L);
        assertThat(result.getList().get(0).getClaimedCount()).isEqualTo(2);
        assertThat(result.getList().get(0).getReadyCount()).isEqualTo(1);
        assertThat(result.getList().get(0).getWaitingCount()).isEqualTo(1);
        assertThat(result.getList().get(0).getNickname()).isEqualTo("成员2");
    }

    @Test
    void emptyOrganizationDoesNotQueryTasks() {
        when(memberQueryBuilder.buildWorkloadMemberQuery(any())).thenReturn(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>());
        when(organizationMemberMapper.selectList(any())).thenReturn(List.of());

        PageResultBO<OrgMemberWorkloadResultBO> result = handler.list(params(SortDirection.DESC, 1, 10));

        assertThat(result.getTotal()).isZero();
        verify(claimedTaskLoadService, never()).listClaimedTasks(any());
    }

    @Test
    void trimsNoteAndClearsBlank() {
        when(organizationMemberMapper.selectById(2L)).thenReturn(member(2L, 22L));

        handler.updateNote(OrgMemberNoteParamsBO.builder().orgId(1L).memberId(2L).note("  这周忙  ").build());

        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<OrganizationMember>> captor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class);
        verify(organizationMemberMapper).update(isNull(), captor.capture());
        assertThat(captor.getValue().getSqlSet()).contains("note");
        assertThat(captor.getValue().getParamNameValuePairs().values()).contains("这周忙");
    }

    @Test
    void missingMemberCannotTakeNote() {
        when(organizationMemberMapper.selectById(9L)).thenReturn(null);

        assertThatThrownBy(() -> handler.updateNote(OrgMemberNoteParamsBO.builder()
                .orgId(1L).memberId(9L).note("备注").build()))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getCode())
                .isEqualTo(ResultCode.DATA_NOT_EXIT.getCode());
    }

    @Test
    void otherOrganizationCannotTakeNote() {
        OrganizationMember member = member(2L, 22L);
        member.setOrgId(8L);
        when(organizationMemberMapper.selectById(2L)).thenReturn(member);

        assertThatThrownBy(() -> handler.updateNote(OrgMemberNoteParamsBO.builder()
                .orgId(1L).memberId(2L).note("备注").build()))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getCode())
                .isEqualTo(ResultCode.UNAUTHORIZED_OPERATION.getCode());
    }

    private static OrgMemberWorkloadListParamsBO params(SortDirection orderDir, int pageNum, int pageSize) {
        return OrgMemberWorkloadListParamsBO.builder()
                .orgId(1L)
                .orderDir(orderDir)
                .page(new PageParamsBO(pageNum, pageSize))
                .build();
    }

    private static OrganizationMember member(long id, long userId) {
        return OrganizationMember.builder().id(id).userId(userId).orgId(1L).status(Status.ENABLED).note("").build();
    }

    private static ClaimedTaskLoadResultBO task(long assigneeId, long instanceId, String waitingStageName) {
        return ClaimedTaskLoadResultBO.builder()
                .assigneeId(assigneeId)
                .instanceId(instanceId)
                .waitingStageName(waitingStageName)
                .build();
    }
}
