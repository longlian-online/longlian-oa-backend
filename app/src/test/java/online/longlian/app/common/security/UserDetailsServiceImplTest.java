package online.longlian.app.common.security;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.mapper.GroupApplicationMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.entity.User;
import online.longlian.common.enumeration.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void shouldReportPendingGroupApplicationBeforeResolvingOrganization() {
        User user = User.builder().id(1L).username("pendinguser").status(Status.DISABLED).build();
        when(userMapper.selectOne(any())).thenReturn(user);
        when(groupApplicationMapper.selectCount(any())).thenReturn(1L);

        UserDetailsServiceImpl service = new UserDetailsServiceImpl(userMapper, groupApplicationMapper);

        assertThatThrownBy(() -> service.loadUserByUsernameOnly("pendinguser"))
                .isInstanceOf(AppException.class)
                .hasMessage("入组申请审批中，请耐心等待");
    }

    @Test
    void shouldLoadEnabledUserWithoutAuthoritiesOrMembership() {
        when(userMapper.selectById(1L)).thenReturn(
                User.builder().id(1L).username("user").status(Status.ENABLED).build());
        UserDetailsServiceImpl service = new UserDetailsServiceImpl(userMapper, groupApplicationMapper);

        UserDetails details = service.loadUserById(1L);

        assertThat(details.getAuthorities()).isEmpty();
        verifyNoInteractions(groupApplicationMapper);
    }
    @Test
    void shouldLoadUserByUsernameAndRejectMissingEmailLookup() {
        when(userMapper.selectOne(any()))
                .thenReturn(User.builder().id(2L).username("user").status(Status.ENABLED).build())
                .thenReturn(null);
        UserDetailsServiceImpl service = new UserDetailsServiceImpl(userMapper, groupApplicationMapper);

        assertThat(service.loadUserByUsername("user").getUsername()).isEqualTo("user");
        assertThatThrownBy(() -> service.loadUserByUsername("missing"))
                .isInstanceOf(AppException.class)
                .hasMessage("用户不存在");
        assertThatThrownBy(() -> service.loadUserByEmailOnly("missing@example.com"))
                .isInstanceOf(AppException.class)
                .hasMessage("用户不存在");
    }

}
