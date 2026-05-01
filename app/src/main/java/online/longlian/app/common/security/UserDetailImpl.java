package online.longlian.app.common.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.generator.enumeration.Status;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDetailImpl implements UserDetails {

    private Long id;

    private String username;

    @JsonIgnore
    private String password;

    private String email;

    private Status status;

    private Long currentOrgId;

    private List<String> roles;

    private List<String> permissions;

    private List<GrantedAuthority> authorities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities != null ? this.authorities : List.of();
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username != null ? this.username : this.email;
    }

    @Override
    public boolean isEnabled() {
        return this.status == Status.ENABLED;
    }
}
