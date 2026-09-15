package online.longlian.app.common.security;

import online.longlian.app.common.exception.AppException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserContextTest {

    private final CurrentUserContext context = new CurrentUserContext();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requireUserIdReturnsAuthenticatedUserId() {
        UserDetailImpl user = UserDetailImpl.builder().id(42L).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null));

        assertThat(context.requireUserId()).isEqualTo(42L);
        assertThat(context.requireUser()).isSameAs(user);
    }

    @Test
    void getAdminIdReturnsAuthenticatedAdminId() {
        AdminUserDetails admin = AdminUserDetails.from(7L, "admin", "root");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null));

        assertThat(context.getAdminId()).isEqualTo(7L);
    }

    @Test
    void getAdminIdReturnsNullForUserOrMissingAuthentication() {
        assertThat(context.getAdminId()).isNull();
        UserDetailImpl user = UserDetailImpl.builder().id(42L).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null));
        assertThat(context.getAdminId()).isNull();
    }

    @Test
    void requireUserRejectsMissingOrWrongPrincipal() {
        assertThatThrownBy(context::requireUser)
                .isInstanceOf(AppException.class);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymous", null));
        assertThatThrownBy(context::requireUserId)
                .isInstanceOf(AppException.class);
    }
}
