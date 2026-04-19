package online.longlian.app.service.user.impl;

import com.alibaba.fastjson2.JSON;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.CommonConstants;
import online.longlian.app.common.constants.RedisConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.Result;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.security.EmailCodeAuthenticationToken;
import online.longlian.app.common.security.MyUsernamePasswordAuthenticationToken;
import online.longlian.app.common.security.UserDetailImpl;
import online.longlian.app.common.util.JwtUtil;
import online.longlian.app.common.util.RedisBlacklistUtil;
import online.longlian.app.pojo.dto.app.LoginByCodeDTO;
import online.longlian.app.pojo.dto.app.LoginByPwdDTO;
import online.longlian.app.pojo.vo.app.LoginVO;
import online.longlian.app.service.user.SessionService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final RedisBlacklistUtil redisBlacklistUtil;
    private final AuthenticationManager authenticationManager;
    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtUtil jwtUtil;

    @Override
    public Result<LoginVO> loginByPwd(LoginByPwdDTO loginByPwdDTO) {
        Authentication authentication =
                authenticationManager.authenticate(
                        new MyUsernamePasswordAuthenticationToken(
                                loginByPwdDTO.getUsername(),
                                loginByPwdDTO.getPassword()
                        )
                );
        return doLogin(authentication);
    }

    @Override
    public Result<LoginVO> loginByCode(LoginByCodeDTO loginByCodeDTO) {
        Authentication authentication =
                authenticationManager.authenticate(
                        new EmailCodeAuthenticationToken(
                                loginByCodeDTO.getEmail(),
                                loginByCodeDTO.getCode()
                        )
                );
        return doLogin(authentication);
    }

    @Override
    public Result<Void> logout(HttpServletRequest request) {
        String token = (String) request.getAttribute(CommonConstants.CURRENT_TOKEN);
        if (token == null) {
            throw new AppException(ResultCode.UNAUTHORIZED);
        }
        long remainingSeconds = jwtUtil.getRemainingTimeSeconds(token);
        redisBlacklistUtil.addToBlacklist(token, remainingSeconds);

        Long userId = getCurrentUserId();
        redisTemplate.delete(RedisConstants.LOGIN_USER + userId);

        return Result.success("登出成功");
    }

    @Override
    public UserDetailImpl getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AppException(ResultCode.UNAUTHORIZED);
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetailImpl userDetail)) {
            throw new AppException(ResultCode.UNAUTHORIZED);
        }
        return userDetail;
    }

    @Override
    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    private Result<LoginVO> doLogin(Authentication authentication) {
        UserDetailImpl userDetail = (UserDetailImpl) authentication.getPrincipal();
        Long userId = userDetail.getId();

        String token = jwtUtil.generateToken(userId);

        redisTemplate.opsForValue().set(
                RedisConstants.LOGIN_USER + userId,
                JSON.toJSONString(userDetail),
                RedisConstants.EXPIRE_TIME,
                TimeUnit.SECONDS
        );
        redisTemplate.opsForValue().set(
                RedisConstants.CURRENT_ORG + userId,
                userDetail.getDefaultOrgId(),
                RedisConstants.EXPIRE_TIME,
                TimeUnit.SECONDS
        );
        LoginVO loginVO = LoginVO.builder()
                .userId(userId)
                .token(token)
                .roles(userDetail.getRoles())
                .defaultOrgId(userDetail.getDefaultOrgId())
                .build();
        return Result.success("登录成功", loginVO);
    }
}
