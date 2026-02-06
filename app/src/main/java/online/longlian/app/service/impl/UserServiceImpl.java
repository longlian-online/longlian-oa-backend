package online.longlian.app.service.impl;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.RedisConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.Result;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.util.JwtUtil;
import online.longlian.app.pojo.dto.LoginReqDTO;
import online.longlian.app.service.security.UserDetailImpl;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Result<Map<String, Object>> login(LoginReqDTO loginReqDTO) {
        try {
            // 交给 Spring Security 做认证
            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    loginReqDTO.getUsername(),
                                    loginReqDTO.getPassword()
                            )
                    );
            UserDetailImpl userDetailImpl = (UserDetailImpl) authentication.getPrincipal();

            Long userId = userDetailImpl.getId();

            // 生成 JWT，包含 tokenKey
            String token = jwtUtil.generateToken(userId);

            // Redis 存用户信息
            redisTemplate.opsForValue().set(
                    RedisConstants.LOGIN_USER + userId,
                    JSON.toJSONString(userDetailImpl),
                    RedisConstants.EXPIRE_TIME,
                    TimeUnit.SECONDS
            );
            // Redis 存 token
            redisTemplate.opsForValue().set(
                    RedisConstants.TOKEN + userId,
                    token,
                    RedisConstants.EXPIRE_TIME,
                    TimeUnit.SECONDS
            );
            // 返回给前端
            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            return Result.success("登录成功",result);
        } catch (AuthenticationException e) {
            //捕获 Security 认证异常，包装为自定义 AppException
            throw new AppException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }
    }
}
