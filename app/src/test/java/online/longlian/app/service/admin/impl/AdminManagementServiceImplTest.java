package online.longlian.app.service.admin.impl;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.AdminMapper;
import online.longlian.app.pojo.bo.admin.AdminCreateParamsBO;
import online.longlian.app.pojo.entity.Admin;
import online.longlian.app.service.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminManagementServiceImplTest {
    @Mock private AdminMapper mapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenBlacklistService tokenBlacklistService;
    private AdminManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminManagementServiceImpl(mapper, passwordEncoder, tokenBlacklistService,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
    }

    /** 移除数据库 CHECK 后，内部创建入口仍必须拒绝非法角色且不写库。 */
    @ParameterizedTest
    @ValueSource(strings = {"INVALID", "ROOT", "Normal", ""})
    void shouldRejectInvalidRoleBeforeWriting(String role) {
        assertThatThrownBy(() -> service.createInternal(params(), role))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ResultCode.UNAUTHORIZED.getCode()));
        verifyNoInteractions(mapper, passwordEncoder, tokenBlacklistService);
    }

    /** 两种合法平台角色通过同一个应用校验入口创建，并只保存密码哈希。 */
    @ParameterizedTest
    @ValueSource(strings = {"root", "normal"})
    void shouldCreateAdminWithValidRole(String role) {
        when(passwordEncoder.encode("123456")).thenReturn("encoded");
        doAnswer(invocation -> {
            invocation.getArgument(0, Admin.class).setId(1L);
            return 1;
        }).when(mapper).insert(any(Admin.class));

        assertThat(service.createInternal(params(), role)).isEqualTo(1L);
        ArgumentCaptor<Admin> saved = ArgumentCaptor.forClass(Admin.class);
        verify(mapper).insert(saved.capture());
        assertThat(saved.getValue().getRole()).isEqualTo(role);
        assertThat(saved.getValue().getPassword()).isEqualTo("encoded");
    }

    private AdminCreateParamsBO params() {
        return AdminCreateParamsBO.builder().username("admin").password("123456").build();
    }
}
