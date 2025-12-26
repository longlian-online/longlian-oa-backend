package online.longlian.app.service;

import online.longlian.app.pojo.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 用户信息表 服务类
 * </p>
 *
 * @author longlian
 * @since 2025-12-26
 */
public interface IUserService extends IService<User> {
    User getByIdCached(Long id);
}
