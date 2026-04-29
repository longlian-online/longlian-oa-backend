package online.longlian.app.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.AdminMapper;
import online.longlian.app.pojo.bo.AdminCreateParamsBO;
import online.longlian.app.pojo.bo.AdminListParamsBO;
import online.longlian.app.pojo.bo.AdminListResultBO;
import online.longlian.app.pojo.bo.PageResultBO;
import online.longlian.app.pojo.entity.Admin;
import online.longlian.app.service.admin.AdminService;
import online.longlian.app.service.admin.SessionService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class AdminServiceImpl implements AdminService {

    private static final String ROLE_ROOT = "root";
    private static final String ROLE_NORMAL = "normal";

    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;
    private final SessionService adminSessionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(@NonNull AdminCreateParamsBO params, @NonNull Long operatorId) {
        // 校验操作者权限
        Admin operator = adminMapper.selectById(operatorId);
        if (operator == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "操作者不存在");
        }
        if (!ROLE_ROOT.equals(operator.getRole())) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "只有root管理员可以创建新管理员");
        }

        // 检查用户名是否已存在
        Admin existAdmin = adminMapper.selectOne(
                new LambdaQueryWrapper<Admin>()
                        .eq(Admin::getUsername, params.getUsername())
                        .last("LIMIT 1")
        );
        if (existAdmin != null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "用户名已存在");
        }

        LocalDateTime now = LocalDateTime.now();
        Admin admin = Admin.builder()
                .username(params.getUsername())
                .password(passwordEncoder.encode(params.getPassword()))
                .role(ROLE_NORMAL)
                .createdAt(now)
                .updatedAt(now)
                .build();
        adminMapper.insert(admin);

        return admin.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(@NonNull Long id, @NonNull Long operatorId) {
        // 校验操作者权限
        Admin operator = adminMapper.selectById(operatorId);
        if (operator == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "操作者不存在");
        }
        if (!ROLE_ROOT.equals(operator.getRole())) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "只有root管理员可以删除管理员");
        }

        // 检查被删除的管理员是否存在
        Admin targetAdmin = adminMapper.selectById(id);
        if (targetAdmin == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "管理员不存在");
        }

        // 不能删除root管理员
        if (ROLE_ROOT.equals(targetAdmin.getRole())) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "不能删除root管理员");
        }

        // 不能删除自己
        if (id.equals(operatorId)) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "不能删除自己");
        }

        // 逻辑删除
        adminMapper.update(
                null,
                new LambdaQueryWrapper<Admin>()
                        .eq(Admin::getId, id)
                        .set(Admin::getDeletedAt, LocalDateTime.now())
        );
    }

    @Override
    public PageResultBO<AdminListResultBO> list(@NonNull AdminListParamsBO params) {
        Page<Admin> page = new Page<>(
                params.getPage() != null ? params.getPage().getPageNum() : 1,
                params.getPage() != null ? params.getPage().getPageSize() : 10
        );

        Page<Admin> adminPage = new LambdaQueryWrapper<Admin>()
                .select(Admin::getId, Admin::getUsername, Admin::getRole, Admin::getLastLoginAt, Admin::getCreatedAt)
                .isNull(Admin::getDeletedAt)
                .orderByDesc(Admin::getCreatedAt)
                .page(page);

        List<AdminListResultBO> list = adminPage.getRecords().stream()
                .map(admin -> AdminListResultBO.builder()
                        .id(admin.getId())
                        .username(admin.getUsername())
                        .role(admin.getRole())
                        .lastLoginAt(admin.getLastLoginAt())
                        .createdAt(admin.getCreatedAt())
                        .build())
                .toList();

        return new PageResultBO<>(list, adminPage.getTotal());
    }
}
