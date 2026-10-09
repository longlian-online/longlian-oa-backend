package online.longlian.app.service.common;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.mapper.*;
import online.longlian.app.pojo.entity.*;
import online.longlian.common.enumeration.Status;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrganizationAuthorizationServiceTest {
    private final OrganizationMapper organizations = mock(OrganizationMapper.class);
    private final OrganizationAuthorizationService service = new OrganizationAuthorizationService(organizations);

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Organization.class);
    }

    @Test
    void shouldRejectDisabledOrganizationForGovernance() {
        when(organizations.selectOne(any())).thenReturn(Organization.builder().id(1L).status(Status.DISABLED).build());
        assertThatThrownBy(() -> service.lockOrganization(1L, true)).isInstanceOf(AppException.class);
        assertThat(service.lockOrganization(1L, false).getId()).isEqualTo(1L);
    }
}
