package online.longlian.app.service.orgadmin.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminGetOrganizationInfoResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminUpdateOrganizationInfoParamsBO;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.service.resource.ResourceService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrgAdminOrganizationServiceImplTest {

    @Mock
    private OrganizationMapper organizationMapper;
    @Mock
    private ResourceService resourceService;

    private OrgAdminOrganizationServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), Organization.class);
        service = new OrgAdminOrganizationServiceImpl(organizationMapper, resourceService);
    }

    @Test
    void getOrganizationInfo_withAvatar_returnsFileIdAndUrl() {
        when(organizationMapper.selectById(1L)).thenReturn(Organization.builder()
                .id(1L)
                .name("组织")
                .description("描述")
                .avatarFileId(12345L)
                .build());
        when(resourceService.getResourceReadUrl(12345L)).thenReturn("https://example.com/avatar.png");

        OrgAdminGetOrganizationInfoResultBO result = service.getOrganizationInfo(1L);

        assertThat(result.getAvatarFileId()).isEqualTo(12345L);
        assertThat(result.getAvatarUrl()).isEqualTo("https://example.com/avatar.png");
    }

    @Test
    void getOrganizationInfo_withoutAvatar_returnsNullUrl() {
        when(organizationMapper.selectById(1L)).thenReturn(Organization.builder()
                .id(1L)
                .name("组织")
                .description("描述")
                .build());

        OrgAdminGetOrganizationInfoResultBO result = service.getOrganizationInfo(1L);

        assertThat(result.getAvatarFileId()).isNull();
        assertThat(result.getAvatarUrl()).isNull();
        verifyNoInteractions(resourceService);
    }

    @Test
    void updateOrganizationInfo_withoutAvatarFileId_preservesAvatar() {
        when(organizationMapper.selectById(1L)).thenReturn(
                Organization.builder().id(1L).avatarFileId(12345L).build());

        service.updateOrganizationInfo(updateParams(null));

        verifyNoInteractions(resourceService);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<LambdaUpdateWrapper> wrapperCaptor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(organizationMapper).update(isNull(), wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getSqlSet()).doesNotContain("avatar_file_id");
    }

    @Test
    void updateOrganizationInfo_withZeroAvatarFileId_clearsAvatar() {
        when(organizationMapper.selectById(1L)).thenReturn(
                Organization.builder().id(1L).avatarFileId(12345L).build());

        service.updateOrganizationInfo(updateParams(0L));

        verify(resourceService).bindBizResource(org.mockito.ArgumentMatchers.argThat(resource ->
                resource.getResourceId().equals(0L)
                        && resource.getReplacedResourceId().equals(12345L)
                        && resource.getBizId().equals(1L)
                        && resource.getCreatorId().equals(2L)
                        && resource.getOrgId().equals(1L)));
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<LambdaUpdateWrapper> wrapperCaptor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(organizationMapper).update(isNull(), wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getSqlSet()).contains("avatar_file_id");
    }

    private OrgAdminUpdateOrganizationInfoParamsBO updateParams(Long avatarFileId) {
        return OrgAdminUpdateOrganizationInfoParamsBO.builder()
                .orgId(1L)
                .userId(2L)
                .name("新名称")
                .description("新描述")
                .avatarFileId(avatarFileId)
                .build();
    }
}
