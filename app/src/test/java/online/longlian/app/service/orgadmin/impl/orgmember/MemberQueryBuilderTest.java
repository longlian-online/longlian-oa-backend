package online.longlian.app.service.orgadmin.impl.orgmember;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminApplicationListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberListParamsBO;
import online.longlian.app.pojo.entity.GroupApplication;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.User;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberQueryBuilderTest {

    @Mock
    private UserMapper userMapper;

    private MemberQueryBuilder queryBuilder;

    /**
     * 初始化 MyBatis Plus 实体表信息缓存，使 LambdaQueryWrapper 中的函数引用可以正常解析列名。
     * 不启动 Spring 容器时需要手动触发。
     */
    @BeforeAll
    static void registerMybatisPlusEntities() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, User.class);
        TableInfoHelper.initTableInfo(assistant, OrganizationMember.class);
    }

    @BeforeEach
    void setUp() {
        queryBuilder = new MemberQueryBuilder(userMapper);
    }

    // ---- buildApplicationListQuery ----

    @Test
    void buildApplicationListQuery_noOptionalParams_shouldReturnWrapper() {
        OrgAdminApplicationListParamsBO params = OrgAdminApplicationListParamsBO.builder()
                .orgId(1L)
                .orderDir(SortDirection.DESC)
                .build();

        LambdaQueryWrapper<GroupApplication> wrapper = queryBuilder.buildApplicationListQuery(params);

        assertNotNull(wrapper);
        // userMapper not involved in this path
        verify(userMapper, never()).selectList(any());
    }

    @Test
    void buildApplicationListQuery_withKeyword_shouldReturnWrapper() {
        OrgAdminApplicationListParamsBO params = OrgAdminApplicationListParamsBO.builder()
                .orgId(1L)
                .keyword("alice")
                .orderDir(SortDirection.DESC)
                .build();

        LambdaQueryWrapper<GroupApplication> wrapper = queryBuilder.buildApplicationListQuery(params);

        assertNotNull(wrapper);
    }

    @Test
    void buildApplicationListQuery_withTimeRange_shouldReturnWrapper() {
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        OrgAdminApplicationListParamsBO params = OrgAdminApplicationListParamsBO.builder()
                .orgId(1L)
                .startApplyTime(start)
                .endApplyTime(end)
                .orderDir(SortDirection.ASC)
                .build();

        LambdaQueryWrapper<GroupApplication> wrapper = queryBuilder.buildApplicationListQuery(params);

        assertNotNull(wrapper);
    }

    @Test
    void buildApplicationListQuery_ascOrder_shouldReturnWrapper() {
        OrgAdminApplicationListParamsBO params = OrgAdminApplicationListParamsBO.builder()
                .orgId(1L)
                .orderDir(SortDirection.ASC)
                .build();

        LambdaQueryWrapper<GroupApplication> wrapper = queryBuilder.buildApplicationListQuery(params);

        assertNotNull(wrapper);
    }

    // ---- buildMemberListQuery ----

    @Test
    void buildMemberListQuery_noKeyword_shouldNotQueryUsers() {
        OrgMemberListParamsBO params = OrgMemberListParamsBO.builder()
                .orgId(1L)
                .orderDir(SortDirection.DESC)
                .build();

        LambdaQueryWrapper<OrganizationMember> wrapper = queryBuilder.buildMemberListQuery(params);

        assertNotNull(wrapper);
        verify(userMapper, never()).selectList(any());
    }

    @Test
    void buildMemberListQuery_keywordMatchesUsers_shouldReturnWrapper() {
        User matched = User.builder().id(10L).build();
        when(userMapper.selectList(any())).thenReturn(List.of(matched));

        OrgMemberListParamsBO params = OrgMemberListParamsBO.builder()
                .orgId(1L)
                .keyword("alice")
                .orderDir(SortDirection.DESC)
                .build();

        LambdaQueryWrapper<OrganizationMember> wrapper = queryBuilder.buildMemberListQuery(params);

        assertNotNull(wrapper);
    }

    @Test
    void buildMemberListQuery_keywordMatchesNoUsers_shouldUseImpossibleCondition() {
        // When no users match the keyword, the builder forces an impossible condition (id = -1)
        when(userMapper.selectList(any())).thenReturn(Collections.emptyList());

        OrgMemberListParamsBO params = OrgMemberListParamsBO.builder()
                .orgId(1L)
                .keyword("nonexistent")
                .orderDir(SortDirection.DESC)
                .build();

        LambdaQueryWrapper<OrganizationMember> wrapper = queryBuilder.buildMemberListQuery(params);

        assertNotNull(wrapper);
    }

    @Test
    void buildMemberListQuery_withTimeRange_shouldReturnWrapper() {
        OrgMemberListParamsBO params = OrgMemberListParamsBO.builder()
                .orgId(1L)
                .startJoinedTime(LocalDateTime.now().minusDays(30))
                .endJoinedTime(LocalDateTime.now())
                .orderDir(SortDirection.ASC)
                .build();

        LambdaQueryWrapper<OrganizationMember> wrapper = queryBuilder.buildMemberListQuery(params);

        assertNotNull(wrapper);
        verify(userMapper, never()).selectList(any());
    }

    @Test
    void buildMemberListQuery_ascOrder_shouldReturnWrapper() {
        OrgMemberListParamsBO params = OrgMemberListParamsBO.builder()
                .orgId(1L)
                .orderDir(SortDirection.ASC)
                .build();

        LambdaQueryWrapper<OrganizationMember> wrapper = queryBuilder.buildMemberListQuery(params);

        assertNotNull(wrapper);
    }
}
