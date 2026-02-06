package online.longlian.app.service.user;

import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.dto.LoginByCodeDTO;
import online.longlian.app.pojo.dto.LoginByPwdDTO;
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
    Result<Map<String, Object>> loginByPwd(LoginByPwdDTO loginByPwdDTO);

    Result<Map<String, Object>> loginByCode(LoginByCodeDTO loginByCodeDTO);
}
