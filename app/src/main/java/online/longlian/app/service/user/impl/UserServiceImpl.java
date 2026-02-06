package online.longlian.app.service.user.impl;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.RedisConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.Result;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.util.JwtUtil;
import online.longlian.app.pojo.dto.LoginByCodeDTO;
import online.longlian.app.pojo.dto.LoginByPwdDTO;
import online.longlian.app.common.security.EmailPasswordAuthenticationToken;
import online.longlian.app.common.security.EmailCodeAuthenticationToken;
import online.longlian.app.common.security.UserDetailImpl;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.service.user.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
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
    public Result<Map<String, Object>> loginByPwd(LoginByPwdDTO loginByPwdDTO) {
        try {
            Authentication authentication =
                    authenticationManager.authenticate(
                            new EmailPasswordAuthenticationToken(
                                    loginByPwdDTO.getEmail(),
                                    loginByPwdDTO.getPassword()
                            )
                    );
            return doLogin(authentication);
        } catch (AuthenticationException e) {
            throw new AppException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }
    }

    @Override
    public Result<Map<String, Object>> loginByCode(LoginByCodeDTO loginByCodeDTO) {
        try {
            Authentication authentication =
                    authenticationManager.authenticate(
                            new EmailCodeAuthenticationToken(
                                    loginByCodeDTO.getEmail(),
                                    loginByCodeDTO.getCode()
                            )
                    );
            return doLogin(authentication);
        } catch (AuthenticationException e) {
            throw new AppException(ResultCode.OPERATION_FAIL);
        }
    }

    private Result<Map<String, Object>> doLogin(Authentication authentication) {
        UserDetailImpl userDetail = (UserDetailImpl) authentication.getPrincipal();
        Long userId = userDetail.getId();

        // 生成 JWT
        String token = jwtUtil.generateToken(userId);

        // Redis 存用户信息
        redisTemplate.opsForValue().set(
                RedisConstants.LOGIN_USER + userId,
                JSON.toJSONString(userDetail),
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
        return Result.success("登录成功", result);
    }
}
