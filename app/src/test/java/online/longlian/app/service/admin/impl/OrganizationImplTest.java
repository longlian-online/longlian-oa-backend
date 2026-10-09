package online.longlian.app.service.admin.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.mapper.AdminMapper;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.pojo.bo.admin.AdminOrganizationUpdateStatusParamsBO;
import online.longlian.app.pojo.bo.admin.AdminGenerateCreateOrgInviteCodeParamsBO;
import online.longlian.app.pojo.entity.Admin;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.service.otp.OTPServiceFactory;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.Status;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrganizationImplTest {
    private final OrganizationMapper organizations = mock(OrganizationMapper.class);
    private final AdminMapper admins = mock(AdminMapper.class);
    private final OTPServiceFactory otps = mock(OTPServiceFactory.class);
    private final OrganizationImpl service = new OrganizationImpl(organizations, admins, mock(ResourceService.class), otps);

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Organization.class);
    }

    @Test
    void shouldRejectNormalAdminBeforeGeneratingInvite() {
        when(admins.selectById(2L)).thenReturn(Admin.builder().id(2L).role("normal").build());
        assertThatThrownBy(() -> service.generateCreateOrgInviteCode(new AdminGenerateCreateOrgInviteCodeParamsBO(2L)))
                .isInstanceOf(AppException.class);
        verifyNoInteractions(otps, organizations);
    }

    @Test
    void shouldRejectMissingOperatorBeforeChangingStatus() {
        assertThatThrownBy(() -> service.updateOrgStatus(new AdminOrganizationUpdateStatusParamsBO(1L, Status.DISABLED, 2L)))
                .isInstanceOf(AppException.class);
        verifyNoInteractions(organizations);
    }

    @Test
    void shouldAllowRootToChangeStatus() {
        when(admins.selectById(2L)).thenReturn(Admin.builder().id(2L).role("root").build());
        when(organizations.selectOne(any())).thenReturn(Organization.builder().id(1L).build());
        service.updateOrgStatus(new AdminOrganizationUpdateStatusParamsBO(1L, Status.DISABLED, 2L));
        verify(organizations).update(isNull(), any());
    }

    @Test
    void shouldRejectUnknownOrganization() {
        when(admins.selectById(2L)).thenReturn(Admin.builder().id(2L).role("root").build());
        assertThatThrownBy(() -> service.updateOrgStatus(new AdminOrganizationUpdateStatusParamsBO(1L, Status.DISABLED, 2L)))
                .isInstanceOf(AppException.class);
        verify(organizations, never()).update(any(), any());
    }
}
