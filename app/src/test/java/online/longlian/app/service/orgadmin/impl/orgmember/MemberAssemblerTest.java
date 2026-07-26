package online.longlian.app.service.orgadmin.impl.orgmember;

import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberInfoResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminApplicationInfoResultBO;
import online.longlian.app.pojo.bo.common.ResourceReadUrlGetResultBO;
import online.longlian.app.pojo.entity.GroupApplication;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.ApplicationType;
import online.longlian.common.enumeration.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberAssemblerTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private ResourceService resourceService;

    private MemberAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new MemberAssembler(userMapper, resourceService);
    }

    @Test
    void assembleMembers_emptyList_returnsEmpty() {
        assertThat(assembler.assembleMembers(Collections.emptyList())).isEmpty();
    }

    @Test
    void assembleMembers_withMembers_returnsAssembled() {
        OrganizationMember member = OrganizationMember.builder()
                .id(1L).userId(10L).orgId(100L).orgRole("org_user")
                .joinedAt(LocalDateTime.now()).submitCount(5).status(Status.ENABLED)
                .build();
        User user = User.builder().id(10L).nickname("Alice").username("alice").avatarFileId(200L).build();
        when(userMapper.selectBatchIds(anyList())).thenReturn(List.of(user));
        when(resourceService.getResourceReadUrls(anyList()))
                .thenReturn(Map.of(200L, new ResourceReadUrlGetResultBO("https://cdn/avatar.png", 100L, "avatar/200")));

        List<OrgMemberInfoResultBO> result = assembler.assembleMembers(List.of(member));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNickname()).isEqualTo("Alice");
        assertThat(result.get(0).getAvatarUrl()).isEqualTo("https://cdn/avatar.png");
        assertThat(result.get(0).getSubmitCount()).isEqualTo(5);
    }

    @Test
    void assembleMembers_userNotFound_returnsNullFields() {
        OrganizationMember member = OrganizationMember.builder()
                .id(1L).userId(99L).orgId(100L).orgRole("org_user")
                .joinedAt(LocalDateTime.now()).submitCount(0).status(Status.ENABLED)
                .build();
        when(userMapper.selectBatchIds(anyList())).thenReturn(Collections.emptyList());
        when(resourceService.getResourceReadUrls(anyList())).thenReturn(Collections.emptyMap());

        List<OrgMemberInfoResultBO> result = assembler.assembleMembers(List.of(member));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNickname()).isNull();
        assertThat(result.get(0).getAvatarUrl()).isNull();
    }

    @Test
    void assembleApplications_emptyList_returnsEmpty() {
        assertThat(assembler.assembleApplications(Collections.emptyList())).isEmpty();
    }

    @Test
    void assembleApplications_existingUser_fillsFromUser() {
        GroupApplication app = GroupApplication.builder()
                .id(1L).userId(10L).orgId(100L)
                .applicationType(ApplicationType.EXISTING_USER)
                .createdAt(LocalDateTime.now())
                .build();
        User user = User.builder().id(10L).nickname("Bob").username("bob").avatarFileId(300L).build();
        when(userMapper.selectBatchIds(anyList())).thenReturn(List.of(user));
        when(resourceService.getResourceReadUrls(anyList()))
                .thenReturn(Map.of(300L, new ResourceReadUrlGetResultBO("https://cdn/bob.png", 100L, "avatar/300")));

        List<OrgAdminApplicationInfoResultBO> result = assembler.assembleApplications(List.of(app));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNickname()).isEqualTo("Bob");
        assertThat(result.get(0).getAvatarUrl()).isEqualTo("https://cdn/bob.png");
    }

    @Test
    void assembleApplications_registerType_fillsFromApplication() {
        GroupApplication app = GroupApplication.builder()
                .id(1L).userId(null).orgId(100L)
                .applicationType(ApplicationType.REGISTER)
                .nickname("NewUser").username("newuser")
                .createdAt(LocalDateTime.now())
                .build();
        when(resourceService.getResourceReadUrls(anyList())).thenReturn(Collections.emptyMap());

        List<OrgAdminApplicationInfoResultBO> result = assembler.assembleApplications(List.of(app));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNickname()).isEqualTo("NewUser");
        assertThat(result.get(0).getUsername()).isEqualTo("newuser");
    }
}
