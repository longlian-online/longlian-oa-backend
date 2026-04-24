package online.longlian.app.common.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.entity.User;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

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

    public UserDetailImpl loadUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AppException(ResultCode.USER_NOT_EXIT);
        }
        return buildUserDetails(user);
    }

    private UserDetailImpl buildUserDetails(User user) {
        List<Long> roleIds = userMapper.selectRoleIdsByUserId(user.getId());
        List<String> permissions = userMapper.selectPermissionCodesByRoleIds(roleIds);
        List<GrantedAuthority> authorities = permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        // 查询用户角色
        List<String> roles = userMapper.selectRoleByUserId(user.getId());
        List<GrantedAuthority> roleAuthorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        authorities.addAll(roleAuthorities);
        // 组装 UserDTO
        UserDetailImpl userDetailImpl = new UserDetailImpl();
        BeanUtils.copyProperties(user, userDetailImpl);
        userDetailImpl.setAuthorities(authorities);
        userDetailImpl.setPermissions(permissions);
        userDetailImpl.setRoles(roles);
        return userDetailImpl;
    }
}