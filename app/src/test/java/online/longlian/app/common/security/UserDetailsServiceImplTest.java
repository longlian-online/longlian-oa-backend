package online.longlian.app.common.security;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.mapper.GroupApplicationMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.common.CurrentOrganizationService;
import online.longlian.common.enumeration.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private GroupApplicationMapper groupApplicationMapper;
    @Mock
    private CurrentOrganizationService currentOrganizationService;

    @Test
    void shouldReportPendingGroupApplicationBeforeResolvingOrganization() {
        User user = User.builder().id(1L).username("pendinguser").status(Status.DISABLED).build();
        when(userMapper.selectOne(any())).thenReturn(user);
        when(groupApplicationMapper.selectCount(any())).thenReturn(1L);

        UserDetailsServiceImpl service = new UserDetailsServiceImpl(
                userMapper, groupApplicationMapper, currentOrganizationService);

        assertThatThrownBy(() -> service.loadUserByUsernameOnly("pendinguser"))
                .isInstanceOf(AppException.class)
                .hasMessage("入组申请审批中，请耐心等待");
        verifyNoInteractions(currentOrganizationService);
    }
}
