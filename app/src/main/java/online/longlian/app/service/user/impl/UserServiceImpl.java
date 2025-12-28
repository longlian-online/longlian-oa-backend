package online.longlian.app.service.user.impl;

import online.longlian.app.pojo.entity.User;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.service.user.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户信息表 服务实现类
 * </p>
 *
 * @author longlian
 * @since 2025-12-26
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    /**
     * 查询用户，并缓存到本地缓存（Caffeine）
     * key = 用户id
     */
    @Override
    @Cacheable(cacheNames = "user", key = "#id")
    public User getByIdCached(Long id) {
        System.out.println("【DB查询】User id=" + id);
        return getById(id); // 调用 MyBatis-Plus 自带方法
    }
}
