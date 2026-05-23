package online.longlian.app.service.app;

import com.baomidou.mybatisplus.extension.service.IService;
import online.longlian.app.pojo.bo.app.UserGetJoinOrgInviteInfoParamsBO;
import online.longlian.app.pojo.bo.app.UserGetJoinOrgInviteInfoResultBO;
import online.longlian.app.pojo.bo.app.UserGetMyInfoResultBO;
import online.longlian.app.pojo.bo.app.UserRegisterByInviteParamsBO;
import online.longlian.app.pojo.bo.app.UserSwitchOrgParamsBO;
import online.longlian.app.pojo.bo.app.UserSwitchOrgResultBO;
import online.longlian.app.pojo.entity.User;

/**
 * <p>
 * 系统用户表 服务类
 * </p>
 *
 * @author longlian
 * @since 2026-02-04
 */
public interface UserService extends IService<User> {

    UserGetMyInfoResultBO getMyInfo(Long userId);

    UserSwitchOrgResultBO switchOrg(UserSwitchOrgParamsBO params);

    void registerAndCreateOrganizationByInvite(UserRegisterByInviteParamsBO params);

    void registerAndJoinOrganizationByInvite(UserRegisterByInviteParamsBO params);

    void joinOrganizationByInvite(Long userId, String inviteCode);

    UserGetJoinOrgInviteInfoResultBO getJoinOrgInviteInfo(UserGetJoinOrgInviteInfoParamsBO params);
}
