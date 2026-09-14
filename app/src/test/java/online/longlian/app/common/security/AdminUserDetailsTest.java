package online.longlian.app.common.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class AdminUserDetailsTest {

    @Test
    void shouldCreateAdminDetailsWithoutCredentials() {
        AdminUserDetails userDetails = AdminUserDetails.from(1L, "admin", "SUPER_ADMIN");

        Assertions.assertNull(userDetails.getPassword());
        Assertions.assertEquals("admin", userDetails.getUsername());
        Assertions.assertTrue(userDetails.getAuthorities()
                .contains(new SimpleGrantedAuthority(AdminUserDetails.SYSTEM_ADMIN_AUTHORITY)));
        Assertions.assertTrue(userDetails.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
    }
}
