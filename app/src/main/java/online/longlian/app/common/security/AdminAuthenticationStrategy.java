package online.longlian.app.common.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.AdminMapper;
import online.longlian.app.pojo.entity.Admin;
import online.longlian.common.enumeration.TokenType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminAuthenticationStrategy implements AuthenticationStrategy {

    private final AdminMapper adminMapper;

    @Override
    public String supportedType() {
        return TokenType.Admin.name().toLowerCase();
    }

    @Override
    public Authentication authenticate(long subjectId) {
        Admin admin = adminMapper.selectOne(
                new LambdaQueryWrapper<Admin>()
                        .eq(Admin::getId, subjectId)
                        .isNull(Admin::getDeletedAt)
                        .last("LIMIT 1")
        );
        if (admin == null) {
            throw new AppException(ResultCode.UNAUTHORIZED, "管理员不存在或已被删除");
        }

        AdminUserDetails adminUserDetails = AdminUserDetails.from(
                admin.getId(),
                admin.getUsername(),
                admin.getPassword(),
                admin.getRole()
        );
        return new UsernamePasswordAuthenticationToken(
                adminUserDetails, null, adminUserDetails.getAuthorities()
        );
    }
}
