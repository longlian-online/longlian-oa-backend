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
 * 用户端用户服务接口。
 * <p>
 * 负责用户注册、个人信息维护及组织加入流程：
 * <ol>
 *   <li><b>注册 + 创建组织</b>：新用户通过邀请码注册并同时创建新组织，用户自动成为该组织管理员</li>
 *   <li><b>注册 + 加入组织</b>：新用户通过邀请码注册后提交加入1申请（生成
 *       {@link online.longlian.app.pojo.entity.GroupApplication} 待管理员审核）</li>
 *   <li><b>已有用户加入组织</b>：已注册用户通过邀请码提交加入申请</li>
 *   <li><b>组织切换</b>：在多组织间切换当前活跃组织，切换结果通过
 *       {@link online.longlian.app.service.common.CurrentOrganizationService} 持久化</li>
 * </ol>
 * <p>
 * 用户注册和加入流程依赖 {@link online.longlian.app.service.otp.OTPServiceFactory OTP 策略工厂}
 * 完成验证码校验和消费。
 */
public interface UserService extends IService<User> {

    /**
     * 获取当前用户的个人信息（含头像 URL）。
     *
     * @param userId 用户 ID
     * @return 包含用户名、邮箱、昵称、头像及默认组织 ID 的个人信息
     */
    UserGetMyInfoResultBO getMyInfo(Long userId);

    /**
     * 切换当前用户的活跃组织。
     *
     * @param params 包含用户 ID 和目标组织 ID 的切换参数
     * @return 包含目标组织基本信息和当前用户在该组织内角色的切换结果
     */
    UserSwitchOrgResultBO switchOrg(UserSwitchOrgParamsBO params);

    /**
     * 新用户注册并创建组织（邀请制）。
     * <p>
     * 在一个事务内完成三项操作：创建用户 → 创建组织 → 将用户设为组织管理员。
     * 需同时提供邮箱验证码和创建组织邀请码。
     *
     * @param params 包含用户名、密码、邮箱、邮箱验证码、邀请码、组织名称的注册参数
     */
    void registerAndCreateOrganizationByInvite(UserRegisterByInviteParamsBO params);

    /**
     * 新用户注册并以加入申请方式加入已有组织。
     * <p>
     * 创建用户后生成一条 {@code ApplicationType.REGISTER} 类型的
     * {@link online.longlian.app.pojo.entity.GroupApplication 加入申请}，
     * 待组织管理员在管理后台审核。申请中暂存用户的凭据信息，审核通过后才正式创建组织成员关系。
     *
     * @param params 包含用户名、密码、邮箱、邮箱验证码、邀请码的注册参数
     */
    void registerAndJoinOrganizationByInvite(UserRegisterByInviteParamsBO params);

    /**
     * 已有用户通过邀请码提交加入申请加入组织。
     * <p>
     * 生成一条 {@code ApplicationType.EXISTING_USER} 类型的加入申请，待管理员审核。
     * 用户已在组织中或已被禁用时会直接拒绝。
     *
     * @param userId     申请的用户 ID
     * @param inviteCode 组织邀请码
     */
    void joinOrganizationByInvite(Long userId, String inviteCode);

    /**
     * 校验邀请码有效性并返回目标组织预览信息。
     * <p>
     * 用于用户查看邀请对应的组织名称后再决定是否注册/加入。
     *
     * @param params 包含邀请码的查询参数
     * @return 包含组织 ID 和名称的预览信息
     */
    UserGetJoinOrgInviteInfoResultBO getJoinOrgInviteInfo(UserGetJoinOrgInviteInfoParamsBO params);
}
