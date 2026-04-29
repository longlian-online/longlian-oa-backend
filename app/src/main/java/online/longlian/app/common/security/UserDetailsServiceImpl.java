package online.longlian.app.common.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.CurrentOrganizationContextBO;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.common.CurrentOrganizationService;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;
    private final CurrentOrganizationService currentOrganizationService;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, usernameOrEmail)
                .or()
                .eq(User::getEmail, usernameOrEmail));
        if (user == null) {
            throw new AppException(ResultCode.USER_NOT_EXIT);
        }
        return buildUserDetails(user);
    }

    public UserDetails loadUserByUsernameOnly(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user == null) {
            throw new AppException(ResultCode.USER_NOT_EXIT);
        }
        return buildUserDetails(user);
    }

    public UserDetails loadUserByEmailOnly(String email) throws UsernameNotFoundException {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email));
        if (user == null) {
            throw new AppException(ResultCode.USER_NOT_EXIT);
        }
        return buildUserDetails(user);
    }

    private UserDetailImpl buildUserDetails(User user) {
        CurrentOrganizationContextBO currentOrgContext = currentOrganizationService.resolveCurrentOrgContext(
                user.getId(),
                user.getDefaultOrgId()
        );
        List<String> roles = currentOrgContext.getRoles() == null ? List.of() : currentOrgContext.getRoles();
        List<String> permissions = List.of();
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.addAll(roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList());

        UserDetailImpl userDetailImpl = new UserDetailImpl();
        BeanUtils.copyProperties(user, userDetailImpl);
        userDetailImpl.setAuthorities(authorities);
        userDetailImpl.setPermissions(permissions);
        userDetailImpl.setCurrentOrgId(currentOrgContext.getOrgId());
        userDetailImpl.setRoles(roles);
        return userDetailImpl;
    }
}
