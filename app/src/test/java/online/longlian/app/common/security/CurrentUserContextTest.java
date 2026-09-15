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
    }

    @Test
    void requireUserIdRejectsMissingOrWrongPrincipal() {
        assertThatThrownBy(context::requireUserId)
                .isInstanceOf(AppException.class);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymous", null));
        assertThatThrownBy(context::requireUserId)
                .isInstanceOf(AppException.class);
    }
}
