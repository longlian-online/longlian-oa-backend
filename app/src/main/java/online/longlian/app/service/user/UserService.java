package online.longlian.app.service.user;

import com.baomidou.mybatisplus.extension.service.IService;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.bo.UserGetJoinOrgInviteInfoParamsBO;
import online.longlian.app.pojo.bo.UserGetJoinOrgInviteInfoResultBO;
import online.longlian.app.pojo.bo.UserRegisterByInviteParamsBO;
import online.longlian.app.pojo.bo.UserSwitchOrgParamsBO;
import online.longlian.app.pojo.bo.UserSwitchOrgResultBO;
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

    Result<UserInfoVO> getMyInfo();

    UserSwitchOrgResultBO switchOrg(UserSwitchOrgParamsBO params);

    void registerAndCreateOrganizationByInvite(UserRegisterByInviteParamsBO params);

    void registerAndJoinOrganizationByInvite(UserRegisterByInviteParamsBO params);

    UserGetJoinOrgInviteInfoResultBO getJoinOrgInviteInfo(UserGetJoinOrgInviteInfoParamsBO params);
}
