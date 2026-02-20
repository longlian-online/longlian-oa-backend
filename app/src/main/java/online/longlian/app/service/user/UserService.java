package online.longlian.app.service.user;

import jakarta.servlet.http.HttpServletRequest;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.dto.LoginByCodeDTO;
import online.longlian.app.pojo.dto.LoginByPwdDTO;
import online.longlian.app.pojo.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import online.longlian.app.pojo.vo.LoginVO;

/**
 * <p>
 * 系统用户表 服务类
 * </p>
 *
 * @author longlian
 * @since 2026-02-04
 */
public interface UserService extends IService<User> {
    Result<LoginVO> loginByPwd(LoginByPwdDTO loginByPwdDTO);

    Result<LoginVO> loginByCode(LoginByCodeDTO loginByCodeDTO);

    Result<Void> logout(HttpServletRequest request);
}
