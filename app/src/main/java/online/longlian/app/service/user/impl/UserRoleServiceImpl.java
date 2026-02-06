package online.longlian.app.service.user.impl;

import online.longlian.app.pojo.entity.UserRole;
import online.longlian.app.mapper.UserRoleMapper;
import online.longlian.app.service.user.UserRoleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户角色关联表 服务实现类
 * </p>
 *
 * @author longlian
 * @since 2026-02-04
 */
@Service
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole> implements UserRoleService {

}
