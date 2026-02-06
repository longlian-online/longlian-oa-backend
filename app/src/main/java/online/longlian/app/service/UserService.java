package online.longlian.app.service;

import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.dto.LoginReqDTO;
import online.longlian.app.pojo.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * <p>
 * 系统用户表 服务类
 * </p>
 *
 * @author longlian
 * @since 2026-02-04
 */
public interface UserService extends IService<User> {
    Result<Map<String, Object>> login(LoginReqDTO loginReqDTO);
}
