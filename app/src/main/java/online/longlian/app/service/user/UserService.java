package online.longlian.app.service.user;

import com.baomidou.mybatisplus.extension.service.IService;
import online.longlian.app.pojo.bo.UserSwitchOrgParamsBO;
import online.longlian.app.pojo.bo.UserSwitchOrgResultBO;
import online.longlian.app.pojo.dto.app.JoinByInviteCodeDTO;
import online.longlian.app.pojo.dto.app.RegisterByInviteDTO;
import online.longlian.app.pojo.entity.User;
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

    online.longlian.app.common.result.Result<Void> registerByInvite(RegisterByInviteDTO registerByInviteDTO);

    online.longlian.app.common.result.Result<UserInfoVO> getMyInfo();

    online.longlian.app.common.result.Result<Void> joinByInviteCode(JoinByInviteCodeDTO joinByInviteCodeDTO);

    UserSwitchOrgResultBO switchOrg(UserSwitchOrgParamsBO params);
}
