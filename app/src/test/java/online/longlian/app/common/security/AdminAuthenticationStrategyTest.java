package online.longlian.app.common.security;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.mapper.AdminMapper;
import online.longlian.app.pojo.entity.Admin;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminAuthenticationStrategyTest {
    @BeforeAll
    static void initializeTableMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Admin.class);
    }

    private final AdminMapper mapper = mock(AdminMapper.class);
    private final AdminAuthenticationStrategy strategy = new AdminAuthenticationStrategy(mapper);

    /** JWT 认证不应将已存储的密码哈希传入认证主体。 */
    @Test
    void shouldAuthenticateWithoutRetainingPassword() {
        when(mapper.selectOne(any())).thenReturn(Admin.builder()
                .id(1L).username("admin").password("stored-hash").role("root").build());
        Authentication authentication = strategy.authenticate(1L, "session-1");
        AdminUserDetails principal = (AdminUserDetails) authentication.getPrincipal();
        assertThat(principal.getPassword()).isNull();
        assertThat(authentication.getCredentials()).isNull();
        assertThat(principal.toString()).doesNotContain("stored-hash");
        assertThat(authentication.isAuthenticated()).isTrue();
    }

    /** 逻辑删除的管理员仍不得通过认证。 */
    @Test
    void shouldRejectMissingAdmin() {
        assertThatThrownBy(() -> strategy.authenticate(1L, "session-1")).isInstanceOf(AppException.class);
    }
}
