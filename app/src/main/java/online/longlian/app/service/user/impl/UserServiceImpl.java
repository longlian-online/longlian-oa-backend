package online.longlian.app.service.user.impl;

import com.alibaba.fastjson2.JSON;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.CommonConstants;
import online.longlian.app.common.constants.RedisConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.Result;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.util.JwtUtil;
import online.longlian.app.common.util.RedisBlacklistUtil;
import online.longlian.app.common.util.ThreadLocalUtil;
import online.longlian.app.pojo.dto.LoginByCodeDTO;
import online.longlian.app.pojo.dto.LoginByPwdDTO;
import online.longlian.app.common.security.MyUsernamePasswordAuthenticationToken;
import online.longlian.app.common.security.EmailCodeAuthenticationToken;
import online.longlian.app.common.security.UserDetailImpl;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.vo.LoginVO;
import online.longlian.app.service.user.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisBlacklistUtil redisBlacklistUtil;
    @Override
    public Result<LoginVO> loginByPwd(LoginByPwdDTO loginByPwdDTO) {
        try {
            Authentication authentication =
                    authenticationManager.authenticate(
                            new MyUsernamePasswordAuthenticationToken(
                                    loginByPwdDTO.getUsername(),
                                    loginByPwdDTO.getPassword()
                            )
                    );
            return doLogin(authentication);
        } catch (AuthenticationException e) {
            throw new AppException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }
    }

    @Override
    public Result<LoginVO> loginByCode(LoginByCodeDTO loginByCodeDTO) {
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

    private Result<LoginVO> doLogin(Authentication authentication) {
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
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setRoles(userDetail.getRoles());
        return Result.success("登录成功", loginVO);
    }
    @Override
    public Result<Void> logout(HttpServletRequest request) {
        try {
            // 1. 验证令牌有效性
            String token = (String) request.getAttribute(CommonConstants.CURRENT_TOKEN);
            if (token == null) {
                throw new AppException(ResultCode.UNAUTHORIZED);
            }
            // 2. 获取令牌剩余有效期，加入黑名单
            long remainingSeconds = jwtUtil.getRemainingTimeSeconds(token);
            redisBlacklistUtil.addToBlacklist(token, remainingSeconds);

            // 3. 清理用户缓存
            Long userId = ThreadLocalUtil.getUserBO().getId();
            redisTemplate.delete(RedisConstants.LOGIN_USER + userId);

            return Result.success("登出成功");
        } catch (Exception e) {
            throw new AppException(ResultCode.OPERATION_FAIL);
        }
    }
}
