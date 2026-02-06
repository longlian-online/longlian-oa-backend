package online.longlian.app.common.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
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
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 查询用户信息
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("email", email));
        if (user == null) {
            throw new AppException();
        }
        // 查询用户权限
        List<String> permissions = userMapper.selectPermissionByUserId(user.getId());
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

        return userDetailImpl;
    }
}
