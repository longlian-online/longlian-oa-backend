package online.longlian.app.service.admin.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import online.longlian.app.common.constants.RedisConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.util.JwtUtil;
import online.longlian.app.common.util.RedisBlacklistUtil;
import online.longlian.app.mapper.AdminMapper;
import online.longlian.app.pojo.bo.AdminLoginParamsBO;
import online.longlian.app.pojo.bo.AdminLoginResultBO;
import online.longlian.app.pojo.bo.AdminLogoutParamsBO;
import online.longlian.app.pojo.bo.AdminSessionCacheBO;
import online.longlian.app.pojo.entity.Admin;
import online.longlian.app.service.admin.SessionService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@AllArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisBlacklistUtil redisBlacklistUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminLoginResultBO login(@NonNull AdminLoginParamsBO params) {
        // 查询管理员
        Admin admin = adminMapper.selectOne(
                new LambdaQueryWrapper<Admin>()
                        .eq(Admin::getUsername, params.getUsername())
                        .isNull(Admin::getDeletedAt)
                        .last("LIMIT 1")
        );

        if (admin == null) {
            throw new AppException(ResultCode.USER_NOT_EXIT, "管理员不存在");
        }

        // 验证密码
        if (!passwordEncoder.matches(params.getPassword(), admin.getPassword())) {
            throw new AppException(ResultCode.PARAM_ERROR, "密码错误");
        }

        // 更新最后登录时间
        LocalDateTime now = LocalDateTime.now();

        adminMapper.update(
                null,
                new LambdaUpdateWrapper<Admin>()
                        .eq(Admin::getId, admin.getId())
                        .set(Admin::getLastLoginAt, now)
                        .set(Admin::getUpdatedAt, now)
        );

        // 生成token
        String token = jwtUtil.generateToken(admin.getId());

        return AdminLoginResultBO.builder()
                .adminId(admin.getId())
                .username(admin.getUsername())
                .role(admin.getRole())
                .token(token)
                .build();
    }

    @Override
    public void logout(@NonNull AdminLogoutParamsBO params) {
        if (params.getToken() == null) {
            throw new AppException(ResultCode.UNAUTHORIZED);
        }
        // 将token加入黑名单
        long remainingSeconds = jwtUtil.getRemainingTimeSeconds(params.getToken());
        redisBlacklistUtil.addToBlacklist(params.getToken(), remainingSeconds);

    }

    @Override
    public Long getCurrentAdminId() {
        // 尝试从Security上下文获取
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long adminId) {
            return adminId;
        }

        // 如果Security上下文没有，则返回null（后续可扩展从token解析）
        return null;
    }
}
