package online.longlian.app.common.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class AdminUserDetailsTest {

    @Test
    void shouldCreateAdminDetailsWithCredentialsAndAuthorities() {
        AdminUserDetails userDetails = AdminUserDetails.from(1L, "admin", "encoded-password", "SUPER_ADMIN");

        Assertions.assertEquals("encoded-password", userDetails.getPassword());
        Assertions.assertEquals("admin", userDetails.getUsername());
        Assertions.assertTrue(userDetails.getAuthorities()
                .contains(new SimpleGrantedAuthority(AdminUserDetails.SYSTEM_ADMIN_AUTHORITY)));
        Assertions.assertTrue(userDetails.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
    }
}
