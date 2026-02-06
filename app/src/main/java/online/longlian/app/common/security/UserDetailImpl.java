package online.longlian.app.common.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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

    private String nickname;

    private String email;

    private Integer status;

    private List<GrantedAuthority> authorities;
    @JsonIgnore
    private boolean accountNonExpired = true;
    @JsonIgnore
    private boolean accountNonLocked = true;
    @JsonIgnore
    private boolean credentialsNonExpired = true;
    @JsonIgnore
    private boolean enabled;
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
        return this.email;
    }
    @Override
    public boolean isEnabled() {
        return this.status != null && this.status == 1;
    }
}
