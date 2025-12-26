package online.longlian.app.service.impl;

import online.longlian.app.pojo.entity.User;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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

}
