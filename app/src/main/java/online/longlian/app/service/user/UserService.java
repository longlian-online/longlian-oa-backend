package online.longlian.app.service.user;

import jakarta.servlet.http.HttpServletRequest;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.dto.app.JoinByInviteCodeDTO;
import online.longlian.app.pojo.dto.app.LoginByCodeDTO;
import online.longlian.app.pojo.dto.app.LoginByPwdDTO;
import online.longlian.app.pojo.dto.app.RegisterByInviteDTO;
import online.longlian.app.pojo.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import online.longlian.app.pojo.vo.app.LoginVO;
import online.longlian.app.pojo.vo.app.UserInfoVO;

/**
 * <p>
 * 系统用户表 服务类
 * </p>
 *
 * @author longlian
 * @since 2026-02-04
 */
public interface UserService extends IService<User> {

    Result<Void> registerByInvite(RegisterByInviteDTO registerByInviteDTO);

    Result<UserInfoVO> getMyInfo();



    Result<Void> joinByInviteCode(JoinByInviteCodeDTO joinByInviteCodeDTO);
}
